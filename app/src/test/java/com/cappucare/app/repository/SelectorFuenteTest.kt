package com.cappucare.app.repository

import com.cappucare.app.data.MockData
import com.cappucare.app.data.ModoOperacion
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.remote.TipoError
import com.cappucare.app.data.repository.FuenteDatos
import com.cappucare.app.data.repository.FuenteLocal
import com.cappucare.app.data.repository.ResultadoLogin
import com.cappucare.app.data.repository.ResultadoMovimiento
import com.cappucare.app.data.repository.SelectorFuente
import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import com.cappucare.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fuente que solo anota qué le pidieron: sirve para saber a cuál delegó el selector. */
private class FuenteAnotadora(private val nombre: String) : FuenteDatos {
    val llamadas = mutableListOf<String>()

    override val productos: Flow<List<Producto>> = flowOf(listOf(MockData.productos.first().copy(nombre = "de $nombre")))
    override val movimientos: Flow<List<Movimiento>> = flowOf(emptyList())

    override suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin {
        llamadas += "iniciarSesion"
        return ResultadoLogin.Exito(MockData.usuarios[0])
    }

    override suspend fun cerrarSesion() {
        llamadas += "cerrarSesion"
    }

    override suspend fun sincronizar(): ApiResult<Unit> {
        llamadas += "sincronizar"
        return ApiResult.Success(Unit)
    }

    override suspend fun guardarProducto(producto: Producto): ApiResult<Producto> {
        llamadas += "guardarProducto"
        return ApiResult.Success(producto)
    }

    override suspend fun cambiarEstado(productoId: String, nuevoEstado: EstadoProducto): ApiResult<Producto> {
        llamadas += "cambiarEstado"
        return ApiResult.Success(MockData.productos.first())
    }

    override suspend fun registrarMovimiento(
        productoId: String, tipo: TipoMovimiento, cantidad: Int, motivo: MotivoSalida?, observacion: String, responsable: Usuario
    ): ApiResult<ResultadoMovimiento> {
        llamadas += "registrarMovimiento"
        return ApiResult.Error("anotado de $nombre", tipo = TipoError.DESCONOCIDO)
    }
}

/**
 * El selector elige la fuente de datos según el modo vigente, en cada operación, y los datos de
 * los dos modos son independientes.
 */
class SelectorFuenteTest {

    private val local = FuenteAnotadora("local")
    private val servidor = FuenteAnotadora("servidor")

    // ---------------------------------------------------------------- elección de la fuente

    @Test
    fun conModoLocalTodasLasOperacionesVanALaFuenteLocal() = runBlocking {
        val s = SelectorFuente(ModoOperacion(modoLocalInicial = true), local, servidor)
        s.iniciarSesion("encargado@cappucare.cl", "encargado123")
        s.guardarProducto(MockData.productos.first())
        s.cambiarEstado("P1", EstadoProducto.VENCIDO)
        s.registrarMovimiento("P1", TipoMovimiento.INGRESO, 1, null, "", MockData.usuarios[0])
        s.sincronizar()
        s.cerrarSesion()
        assertEquals(
            listOf("iniciarSesion", "guardarProducto", "cambiarEstado", "registrarMovimiento", "sincronizar", "cerrarSesion"),
            local.llamadas
        )
        assertTrue(servidor.llamadas.isEmpty())
    }

    @Test
    fun conModoServidorTodasLasOperacionesVanALaFuenteDelServidor() = runBlocking {
        val s = SelectorFuente(ModoOperacion(modoLocalInicial = false), local, servidor)
        s.iniciarSesion("encargado@cappucare.cl", "encargado123")
        s.guardarProducto(MockData.productos.first())
        s.sincronizar()
        s.cerrarSesion()
        assertEquals(listOf("iniciarSesion", "guardarProducto", "sincronizar", "cerrarSesion"), servidor.llamadas)
        assertTrue(local.llamadas.isEmpty())
    }

    @Test
    fun elCambioDeModoEnCalienteSeNotaEnLaSiguienteOperacion() = runBlocking {
        val modo = ModoOperacion(modoLocalInicial = false)
        val s = SelectorFuente(modo, local, servidor)
        s.sincronizar()
        modo.establecer(true)
        s.sincronizar()
        modo.establecer(false)
        s.sincronizar()
        assertEquals(2, servidor.llamadas.size)
        assertEquals(1, local.llamadas.size)
    }

    @Test
    fun losFlujosDeDatosCambianDeOrigenAlCambiarElModo() = runBlocking {
        val modo = ModoOperacion(modoLocalInicial = true)
        val s = SelectorFuente(modo, local, servidor)
        assertEquals("de local", s.productos.first().first().nombre)
        modo.establecer(false)
        assertEquals("de servidor", s.productos.first().first().nombre)
    }

    @Test
    fun elSelectorDevuelveElResultadoDeLaFuenteElegida() = runBlocking {
        val r = SelectorFuente(ModoOperacion(true), local, servidor)
            .registrarMovimiento("P1", TipoMovimiento.INGRESO, 1, null, "", MockData.usuarios[0])
        assertEquals("anotado de local", (r as ApiResult.Error).mensaje)
    }

    // ---------------------------------------------------------------- con la fuente local real

    private class Entorno {
        val almacenLocal = AlmacenEnMemoria()
        val almacenServidor = AlmacenEnMemoria()      // en la app real es la copia Room del servidor
        val modo = ModoOperacion(modoLocalInicial = true)
        val selector = SelectorFuente(modo, FuenteLocal(almacenLocal), FuenteLocal(almacenServidor))
    }

    @Test
    fun loLocalYLoDelOtroModoSonIndependientesEnAmbosSentidos() = runBlocking {
        val e = Entorno()
        val encargado = MockData.usuarios[0]
        val nuevo = Producto("", "LOC-001", "Producto solo local", "", CategoriaInsumo.OTRO, UnidadMedida.UNIDAD, nivelMinimo = 1, ubicacion = "X1")
        assertTrue(e.selector.guardarProducto(nuevo) is ApiResult.Success)
        e.selector.registrarMovimiento("P2", TipoMovimiento.INGRESO, 10, null, "", encargado)
        assertEquals(9, e.selector.productos.first().size)

        e.modo.establecer(false)
        assertEquals(8, e.selector.productos.first().size)
        assertEquals(8, e.selector.productos.first().first { it.id == "P2" }.cantidadDisponible)
        e.selector.registrarMovimiento("P3", TipoMovimiento.INGRESO, 4, null, "", encargado)
        assertEquals(16, e.selector.productos.first().first { it.id == "P3" }.cantidadDisponible)

        e.modo.establecer(true)
        assertEquals(12, e.selector.productos.first().first { it.id == "P3" }.cantidadDisponible)
        assertEquals(18, e.selector.productos.first().first { it.id == "P2" }.cantidadDisponible)
    }

    @Test
    fun lasEscriturasLocalesFuncionanEnVariosPasosSeguidosYElHistorialCrece() = runBlocking {
        val e = Entorno()
        val vendedor = MockData.usuarios[1]
        e.selector.registrarMovimiento("P1", TipoMovimiento.SALIDA, 10, MotivoSalida.VENTA, "", vendedor)
        e.selector.registrarMovimiento("P1", TipoMovimiento.SALIDA, 30, MotivoSalida.VENTA, "", vendedor)
        val p1 = e.selector.productos.first().first { it.id == "P1" }
        assertEquals(0, p1.cantidadDisponible)
        assertEquals(EstadoProducto.AGOTADO, p1.estado)
        assertEquals(13, e.selector.movimientos.first().size)
        val rechazo = e.selector.registrarMovimiento("P1", TipoMovimiento.SALIDA, 1, MotivoSalida.VENTA, "", vendedor)
        assertEquals("Solo hay 0 disponibles", (rechazo as ApiResult.Error).mensaje)
    }
}
