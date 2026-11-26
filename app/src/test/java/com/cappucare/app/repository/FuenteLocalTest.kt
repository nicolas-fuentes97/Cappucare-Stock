package com.cappucare.app.repository

import com.cappucare.app.data.MockData
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.remote.TipoError
import com.cappucare.app.data.repository.FuenteLocal
import com.cappucare.app.data.repository.ResultadoLogin
import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Perfil
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * MODO LOCAL: la fuente que opera solo con la base del equipo (aquí, una copia en memoria) aplica
 * las mismas reglas de negocio que el servidor y devuelve los mismos tipos de error.
 */
class FuenteLocalTest {

    private lateinit var almacen: AlmacenEnMemoria
    private lateinit var fuente: FuenteLocal
    private var contador = 0

    private val encargado get() = MockData.usuarios[0]
    private val vendedor get() = MockData.usuarios[1]

    @Before
    fun preparar() {
        almacen = AlmacenEnMemoria()
        contador = 0
        fuente = FuenteLocal(almacen, nuevoId = { "loc-${++contador}" }, reloj = { "2026-11-23 10:00" })
    }

    private fun nuevoProducto(codigo: String = "CUR-003", nombre: String = "Tela adhesiva") = Producto(
        id = "", codigo = codigo, nombre = nombre, descripcion = "Rollo", categoria = CategoriaInsumo.CURACION,
        unidadMedida = UnidadMedida.ROLLO, nivelMinimo = 5, ubicacion = "Estante A3",
        cantidadDisponible = 99, estado = EstadoProducto.VENCIDO     // la fuente debe ignorarlos al crear
    )

    private fun producto(id: String) = almacen.productosGuardados.first { it.id == id }

    // ---------------------------------------------------------------- login local

    @Test
    fun loginLocalAceptaLaClaveCorrectaConLaMismaSalDelServidor() = runBlocking {
        val r = fuente.iniciarSesion("  ENCARGADO@cappucare.cl ", "encargado123")
        assertTrue(r is ResultadoLogin.Exito)
        r as ResultadoLogin.Exito
        assertEquals(Perfil.ENCARGADO, r.usuario.perfil)
        assertFalse(r.sinConexion)
    }

    @Test
    fun loginLocalRechazaClaveIncorrectaYUsuarioInexistenteConElMismoMensaje() = runBlocking {
        val mala = fuente.iniciarSesion("encargado@cappucare.cl", "otra-clave")
        val inexistente = fuente.iniciarSesion("nadie@cappucare.cl", "encargado123")
        assertEquals(ResultadoLogin.Rechazado("Usuario o clave incorrectos"), mala)
        assertEquals(mala, inexistente)
    }

    // ---------------------------------------------------------------- RF-01 / RF-03

    @Test
    fun crearProductoLocalAsignaIdCantidadCeroYDisponibleYGuardaElCodigoEnMayusculas() = runBlocking {
        val r = fuente.guardarProducto(nuevoProducto(codigo = "cur-003"))
        assertTrue(r is ApiResult.Success)
        val creado = (r as ApiResult.Success).datos
        assertEquals("loc-1", creado.id)
        assertEquals("CUR-003", creado.codigo)
        assertEquals(0, creado.cantidadDisponible)
        assertEquals(EstadoProducto.DISPONIBLE, creado.estado)
        assertEquals(creado, producto("loc-1"))
        assertEquals(9, fuente.productos.first().size)
    }

    @Test
    fun crearProductoConCodigoRepetidoEsConflictoYNoGuardaNada() = runBlocking {
        val r = fuente.guardarProducto(nuevoProducto(codigo = "cur-001"))
        r as ApiResult.Error
        assertEquals(TipoError.CONFLICTO, r.tipo)
        assertEquals("codigo", r.campo)
        assertEquals("Ya existe un producto con ese código", r.mensaje)
        assertEquals(8, almacen.productosGuardados.size)
    }

    @Test
    fun crearProductoInvalidoDevuelveElErrorDelCampo() = runBlocking {
        val r = fuente.guardarProducto(nuevoProducto(nombre = "ab"))
        r as ApiResult.Error
        assertEquals(TipoError.SOLICITUD_INVALIDA, r.tipo)
        assertEquals("nombre", r.campo)
        assertEquals("Mínimo 3 caracteres", r.mensaje)
        assertEquals(8, almacen.productosGuardados.size)
    }

    @Test
    fun editarProductoConservaLaCantidadYElEstadoYPermiteSuPropioCodigo() = runBlocking {
        val original = producto("P1")
        val r = fuente.guardarProducto(original.copy(nombre = "Gasa estéril 20x20 cm", cantidadDisponible = 1, estado = EstadoProducto.AGOTADO))
        assertTrue(r is ApiResult.Success)
        assertEquals("Gasa estéril 20x20 cm", producto("P1").nombre)
        assertEquals(original.cantidadDisponible, producto("P1").cantidadDisponible)
        assertEquals(original.estado, producto("P1").estado)
    }

    @Test
    fun unaFallaDeLaBaseSeVuelveUnErrorConMensajeYNoUnCierreDeLaApp() = runBlocking {
        almacen.fallarAlGuardarProducto = true
        val r = fuente.guardarProducto(nuevoProducto())
        r as ApiResult.Error
        assertTrue(r.mensaje.contains("UNIQUE"))
        assertEquals(8, almacen.productosGuardados.size)
    }

    @Test
    fun cambiarEstadoIncoherenteConLaCantidadEsReglaDeNegocioYNoCambia() = runBlocking {
        val r = fuente.cambiarEstado("P1", EstadoProducto.AGOTADO)    // P1 tiene 40 unidades
        r as ApiResult.Error
        assertEquals(TipoError.REGLA_DE_NEGOCIO, r.tipo)
        assertEquals("estado", r.campo)
        assertEquals(EstadoProducto.DISPONIBLE, producto("P1").estado)
    }

    @Test
    fun cambiarEstadoValidoSeGuarda() = runBlocking {
        assertTrue(fuente.cambiarEstado("P1", EstadoProducto.VENCIDO) is ApiResult.Success)
        assertEquals(EstadoProducto.VENCIDO, producto("P1").estado)
    }

    // ---------------------------------------------------------------- RF-04 / RF-05 / RF-06 / RF-07

    @Test
    fun ingresoSumaStockYRegistraResponsableYFechaDeLaSesion() = runBlocking {
        val r = fuente.registrarMovimiento("P2", TipoMovimiento.INGRESO, 10, null, " reposición ", vendedor)
        assertTrue(r is ApiResult.Success)
        r as ApiResult.Success
        assertEquals(8, r.datos.antes.cantidadDisponible)
        assertEquals(18, r.datos.despues.cantidadDisponible)
        assertEquals(18, producto("P2").cantidadDisponible)
        val m = almacen.movimientosGuardados.first()
        assertEquals("loc-1", m.id)
        assertEquals("2026-11-23 10:00", m.fecha)
        assertEquals(vendedor.nombre, m.responsable)
        assertEquals(Perfil.USUARIO_AUTORIZADO, m.responsablePerfil)
        assertEquals("reposición", m.observacion)
        assertNull(m.motivo)
    }

    @Test
    fun salidaMayorAlStockSeRechazaSinCambiarNada() = runBlocking {
        val r = fuente.registrarMovimiento("P2", TipoMovimiento.SALIDA, 9, MotivoSalida.VENTA, "", vendedor)
        r as ApiResult.Error
        assertEquals(TipoError.REGLA_DE_NEGOCIO, r.tipo)
        assertEquals("cantidad", r.campo)
        assertEquals("Solo hay 8 disponibles", r.mensaje)
        assertEquals(8, producto("P2").cantidadDisponible)
        assertEquals(11, almacen.movimientosGuardados.size)
    }

    @Test
    fun salidaSinMotivoEsSolicitudInvalidaPorRN02() = runBlocking {
        val r = fuente.registrarMovimiento("P2", TipoMovimiento.SALIDA, 1, null, "", vendedor)
        r as ApiResult.Error
        assertEquals(TipoError.SOLICITUD_INVALIDA, r.tipo)
        assertEquals("motivo", r.campo)
    }

    @Test
    fun cantidadCeroEsSolicitudInvalida() = runBlocking {
        val r = fuente.registrarMovimiento("P2", TipoMovimiento.INGRESO, 0, null, "", encargado)
        r as ApiResult.Error
        assertEquals(TipoError.SOLICITUD_INVALIDA, r.tipo)
        assertEquals("cantidad", r.campo)
    }

    @Test
    fun salidaQueDejaElStockEnCeroPasaElProductoAAgotado() = runBlocking {
        val r = fuente.registrarMovimiento("P5", TipoMovimiento.SALIDA, 6, MotivoSalida.VENTA, "", vendedor)
        assertTrue(r is ApiResult.Success)
        assertEquals(0, producto("P5").cantidadDisponible)
        assertEquals(EstadoProducto.AGOTADO, producto("P5").estado)
    }

    @Test
    fun noSeVendeUnProductoVencidoPeroSiSePuedeDarDeBajaPorMerma() = runBlocking {
        fuente.cambiarEstado("P1", EstadoProducto.VENCIDO)
        val venta = fuente.registrarMovimiento("P1", TipoMovimiento.SALIDA, 1, MotivoSalida.VENTA, "", vendedor)
        venta as ApiResult.Error
        assertEquals(TipoError.REGLA_DE_NEGOCIO, venta.tipo)
        assertEquals("motivo", venta.campo)
        assertTrue(fuente.registrarMovimiento("P1", TipoMovimiento.SALIDA, 1, MotivoSalida.MERMA, "", encargado) is ApiResult.Success)
    }

    @Test
    fun movimientoSobreUnProductoInexistenteEsNoEncontrado() = runBlocking {
        val r = fuente.registrarMovimiento("P99", TipoMovimiento.INGRESO, 1, null, "", encargado)
        r as ApiResult.Error
        assertEquals(TipoError.NO_ENCONTRADO, r.tipo)
    }

    // ---------------------------------------------------------------- sin servidor y datos de ejemplo

    @Test
    fun enModoLocalNoHayNadaQueSincronizar() = runBlocking {
        assertTrue(fuente.sincronizar() is ApiResult.Success)
    }

    @Test
    fun restablecerDatosVuelveALosDatosDeEjemplo() = runBlocking {
        fuente.guardarProducto(nuevoProducto())
        fuente.registrarMovimiento("P2", TipoMovimiento.INGRESO, 10, null, "", encargado)
        fuente.restablecerDatos()
        assertEquals(MockData.productos, almacen.productosGuardados)
        assertEquals(MockData.movimientos.size, almacen.movimientosGuardados.size)
        assertEquals(MockData.movimientos.last().id, almacen.movimientosGuardados.first().id)
    }
}
