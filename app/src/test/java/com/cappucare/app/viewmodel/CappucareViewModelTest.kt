package com.cappucare.app.viewmodel

import android.app.Application
import com.cappucare.app.data.MockData
import com.cappucare.app.data.local.PreferenciasUsuario
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.remote.TipoError
import com.cappucare.app.data.repository.FuenteDatos
import com.cappucare.app.data.repository.FuenteLocal
import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Perfil
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import com.cappucare.app.reglas.Validaciones
import com.cappucare.app.repository.AlmacenEnMemoria
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Pruebas unitarias del ViewModel (JUnit 4 + MockK + kotlinx-coroutines-test), sin emulador.
 *
 *  - Se prueba el ViewModel REAL. El repositorio es FuenteLocal con una base EN MEMORIA
 *    (AlmacenEnMemoria) en vez de Room; para los casos de error se usa un repositorio simulado
 *    con MockK. La Application también es un doble de MockK (el ViewModel no la usa en las pruebas).
 *  - viewModelScope usa Dispatchers.Main, que no existe fuera de Android: en cada prueba se
 *    reemplaza por un dispatcher de prueba (UnconfinedTestDispatcher ejecuta las corrutinas al tiro).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CappucareViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var almacen: AlmacenEnMemoria
    private lateinit var preferencias: PreferenciasEnMemoria
    private lateinit var viewModel: CappucareViewModel
    private var contador = 0

    /** Preferencias en memoria (en la app se guardan con DataStore). */
    private class PreferenciasEnMemoria : PreferenciasUsuario {
        val guardado = MutableStateFlow("")
        var fallar = false
        override val ultimoUsuario: Flow<String> = guardado
        override suspend fun guardarUltimoUsuario(usuario: String) {
            if (fallar) throw IOException("sin espacio")
            guardado.value = usuario
        }
    }

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
        almacen = AlmacenEnMemoria()
        preferencias = PreferenciasEnMemoria()
        contador = 0
        val fuente = FuenteLocal(almacen, nuevoId = { "N${++contador}" }, reloj = { "2026-11-25 10:00" })
        viewModel = CappucareViewModel(mockk<Application>(relaxed = true), fuente, preferencias)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------- ayudas

    private fun TestScope.mensajes(vm: CappucareViewModel = viewModel): MutableList<String> {
        val lista = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.mensajes.collect { lista += it } }
        return lista
    }

    private fun ingresarComoEncargado() = viewModel.login("encargado@cappucare.cl", "encargado123")
    private fun ingresarComoVendedor() = viewModel.login("ventas@cappucare.cl", "ventas123")

    private fun nuevoProducto(codigo: String = "CUR-003", nombre: String = "Tela adhesiva") = Producto(
        id = "", codigo = codigo, nombre = nombre, descripcion = "Rollo", categoria = CategoriaInsumo.CURACION,
        unidadMedida = UnidadMedida.ROLLO, nivelMinimo = 5, ubicacion = "Estante A3"
    )

    // ---------------------------------------------------------------- estado inicial

    @Test
    fun alCrearseCargaElCatalogoElHistorialYLasAlertasDeReposicion() {
        assertEquals(8, viewModel.productos.value.size)
        assertEquals(11, viewModel.movimientos.value.size)
        assertEquals(setOf("P2", "P4", "P6"), viewModel.alertasReposicion.value.map { it.id }.toSet())
        assertNull(viewModel.usuarioActivo.value)
        assertFalse(viewModel.ocupado.value)
    }

    @Test
    fun ultimoUsuarioMuestraElCorreoRecordado() {
        preferencias.guardado.value = "ventas@cappucare.cl"
        assertEquals("ventas@cappucare.cl", viewModel.ultimoUsuario.value)
    }

    // ---------------------------------------------------------------- RF-00 inicio de sesión

    @Test
    fun loginCorrectoAbreLaSesionYRecuerdaElUsuario() {
        ingresarComoEncargado()
        assertEquals(Perfil.ENCARGADO, viewModel.usuarioActivo.value?.perfil)
        assertEquals("encargado@cappucare.cl", preferencias.guardado.value)
        assertNull(viewModel.errorLogin.value)
        assertFalse(viewModel.cargandoLogin.value)
        assertTrue(viewModel.puedeEditarCatalogo())
    }

    @Test
    fun loginConClaveIncorrectaMuestraElMensajeGenerico() {
        viewModel.login("encargado@cappucare.cl", "otra-clave")
        assertNull(viewModel.usuarioActivo.value)
        assertEquals(FuenteLocal.MENSAJE_CREDENCIALES, viewModel.errorLogin.value)
        viewModel.limpiarErrorLogin()
        assertNull(viewModel.errorLogin.value)
    }

    @Test
    fun loginConCamposVaciosNoConsultaLaBase() {
        val errores = viewModel.validarLogin("", "")
        assertTrue(errores.hayErrores)
        viewModel.login("", "")
        assertNull(viewModel.usuarioActivo.value)
        assertNull(viewModel.errorLogin.value)
    }

    @Test
    fun siNoSePuedeRecordarElUsuarioIgualSeIngresa() {
        preferencias.fallar = true
        ingresarComoVendedor()
        assertEquals(Perfil.USUARIO_AUTORIZADO, viewModel.usuarioActivo.value?.perfil)
        assertFalse(viewModel.puedeEditarCatalogo())
    }

    @Test
    fun logoutCierraLaSesionYOlvidaElProductoElegido() {
        ingresarComoEncargado()
        viewModel.seleccionarProducto("P1")
        viewModel.logout()
        assertNull(viewModel.usuarioActivo.value)
        assertNull(viewModel.productoSeleccionadoId.value)
    }

    // ---------------------------------------------------------------- RF-01 / RF-02 / RF-08

    @Test
    fun consultasDelCatalogoUsanLasReglasDeInventario() {
        assertEquals("P1", viewModel.productoPorCodigo("cur-001")?.id)
        assertEquals("P1", viewModel.buscarProductos("gasa", null).single().id)
        assertEquals(2, viewModel.buscarProductos("", CategoriaInsumo.PROTECCION).size)
        assertEquals("Gasa estéril 10x10 cm", viewModel.productoPorId("P1")?.nombre)
        assertNull(viewModel.productoPorId(null))
    }

    @Test
    fun validarProductoDetectaElCodigoRepetido() {
        val errores = viewModel.validarProducto(null, "CUR-001", "Gasa", CategoriaInsumo.CURACION, UnidadMedida.PAQUETE, "5", "A1")
        assertEquals(Validaciones.MENSAJE_CODIGO_REPETIDO, errores.codigo)
    }

    @Test
    fun crearProductoComoEncargadoLoAgregaAlCatalogo() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        viewModel.guardarProducto(nuevoProducto())
        assertEquals(9, viewModel.productos.value.size)
        assertEquals("Producto \"Tela adhesiva\" creado", avisos.last())
        assertFalse(viewModel.ocupado.value)
    }

    @Test
    fun editarProductoAvisaCambiosGuardados() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        val p1 = viewModel.productoPorId("P1")!!
        viewModel.guardarProducto(p1.copy(ubicacion = "Estante Z9"))
        assertEquals("Estante Z9", viewModel.productoPorId("P1")?.ubicacion)
        assertEquals("Cambios guardados", avisos.last())
    }

    @Test
    fun siElRepositorioRechazaElProductoSeMuestraSuMensaje() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        viewModel.guardarProducto(nuevoProducto(codigo = "CUR-001"))
        assertEquals(Validaciones.MENSAJE_CODIGO_REPETIDO, avisos.last())
        assertEquals(8, viewModel.productos.value.size)
    }

    @Test
    fun unUsuarioAutorizadoNoPuedeCrearNiCambiarEstadoNiEliminar() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoVendedor()
        viewModel.guardarProducto(nuevoProducto())
        viewModel.cambiarEstado("P1", EstadoProducto.VENCIDO)
        viewModel.eliminarProducto("P1")
        assertEquals(List(3) { CappucareViewModel.MENSAJE_SIN_PERMISO }, avisos)
        assertEquals(8, viewModel.productos.value.size)
        assertEquals(EstadoProducto.DISPONIBLE, viewModel.productoPorId("P1")?.estado)
    }

    // ---------------------------------------------------------------- RF-03 estado

    @Test
    fun cambiarEstadoValidoLoGuarda() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        viewModel.cambiarEstado("P1", EstadoProducto.VENCIDO)
        assertEquals(EstadoProducto.VENCIDO, viewModel.productoPorId("P1")?.estado)
        assertEquals("Estado actualizado a Vencido", avisos.last())
    }

    @Test
    fun cambiarAlMismoEstadoOAUnoIncoherenteAvisaYNoGuarda() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        viewModel.cambiarEstado("P1", EstadoProducto.DISPONIBLE)
        viewModel.cambiarEstado("P1", EstadoProducto.AGOTADO)
        viewModel.cambiarEstado("P99", EstadoProducto.VENCIDO)        // no existe: no hace nada
        assertEquals(2, avisos.size)
        assertTrue(avisos[0].startsWith("Sin cambios"))
        assertTrue(avisos[1].startsWith("Solo un producto sin unidades"))
        assertEquals(EstadoProducto.DISPONIBLE, viewModel.productoPorId("P1")?.estado)
    }

    // ---------------------------------------------------------------- eliminar (CRUD)

    @Test
    fun eliminarUnProductoSinMovimientosLoQuitaYVuelveALaLista() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        viewModel.guardarProducto(nuevoProducto())
        assertTrue(viewModel.puedeEliminar("N1"))
        viewModel.seleccionarProducto("N1")
        var volvio = false
        viewModel.eliminarProducto("N1") { volvio = true }
        assertTrue(volvio)
        assertNull(viewModel.productoPorId("N1"))
        assertNull(viewModel.productoSeleccionadoId.value)
        assertEquals("Producto \"Tela adhesiva\" eliminado", avisos.last())
    }

    @Test
    fun noSeEliminaUnProductoConMovimientos() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        assertFalse(viewModel.puedeEliminar("P1"))
        var volvio = false
        viewModel.eliminarProducto("P1") { volvio = true }
        assertFalse(volvio)
        assertNotNull(viewModel.productoPorId("P1"))
        assertEquals(FuenteLocal.mensajeConMovimientos(2), avisos.last())
    }

    // ---------------------------------------------------------------- RF-04 a RF-07 movimientos

    @Test
    fun sinSesionNoSeRegistranMovimientos() = runTest(dispatcher) {
        val avisos = mensajes()
        viewModel.registrarMovimiento("P1", TipoMovimiento.INGRESO, 5, null, "")
        assertEquals("Debes iniciar sesión para registrar movimientos.", avisos.single())
        assertEquals(11, viewModel.movimientos.value.size)
    }

    @Test
    fun unaVentaQueCruzaElMinimoActualizaElStockYAvisa() = runTest(dispatcher) {
        val avisos = mensajes()
        val avisosStock = mutableListOf<Producto>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.avisoStockMinimo.collect { avisosStock += it } }
        ingresarComoVendedor()
        viewModel.registrarMovimiento("P1", TipoMovimiento.SALIDA, 25, MotivoSalida.VENTA, "")
        assertEquals(15, viewModel.productoPorId("P1")?.cantidadDisponible)
        assertEquals(12, viewModel.movimientos.value.size)
        assertEquals("P1", avisosStock.single().id)
        assertTrue(avisos.last().startsWith("Salida de 25 registrado"))
        assertTrue(viewModel.alertasReposicion.value.any { it.id == "P1" })
    }

    @Test
    fun unIngresoQueNoCruzaElMinimoNoAvisa() = runTest(dispatcher) {
        val avisos = mensajes()
        val avisosStock = mutableListOf<Producto>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.avisoStockMinimo.collect { avisosStock += it } }
        ingresarComoEncargado()
        viewModel.registrarMovimiento("P2", TipoMovimiento.INGRESO, 10, null, "Reposición")
        assertEquals(18, viewModel.productoPorId("P2")?.cantidadDisponible)
        assertTrue(avisosStock.isEmpty())
        assertTrue(avisos.last().startsWith("Ingreso de 10 registrado"))
    }

    @Test
    fun unaSalidaMayorAlStockSeRechaza() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        val errores = viewModel.validarMovimiento("P4", TipoMovimiento.SALIDA, "999", MotivoSalida.VENTA)
        assertNotNull(errores.cantidad)
        viewModel.registrarMovimiento("P4", TipoMovimiento.SALIDA, 999, MotivoSalida.VENTA, "")
        assertEquals(3, viewModel.productoPorId("P4")?.cantidadDisponible)
        assertEquals(errores.cantidad, avisos.last())
    }

    // ---------------------------------------------------------------- RF-09 / RF-10

    @Test
    fun reglasDeReposicionSeAplicanDesdeElViewModel() {
        val p2 = viewModel.productoPorId("P2")!!
        val p1 = viewModel.productoPorId("P1")!!
        assertTrue(viewModel.estaBajoMinimo(p2))
        assertFalse(viewModel.estaBajoMinimo(p1))
        assertEquals(7, viewModel.faltaParaMinimo(p2))
        assertEquals(8f / 15f, viewModel.nivelDeCobertura(p2), 0.001f)
    }

    @Test
    fun historialResumenYCsvDeUnProducto() {
        val historial = viewModel.historialDe("P1")
        assertEquals(listOf("M2", "M1"), historial.map { it.movimiento.id })
        assertEquals(40, historial.first().saldo)
        assertEquals(1, viewModel.historialDe("P1", TipoMovimiento.SALIDA).size)
        val resumen = viewModel.resumenHistorial("P1")
        assertEquals(50, resumen.totalIngresos)
        assertEquals(10, resumen.totalSalidas)
        assertTrue(viewModel.csvHistorial(historial).startsWith("Fecha"))
    }

    // ---------------------------------------------------------------- datos de ejemplo

    @Test
    fun restablecerDatosVuelveALosDeEjemplo() = runTest(dispatcher) {
        val avisos = mensajes()
        ingresarComoEncargado()
        viewModel.guardarProducto(nuevoProducto())
        var termino = false
        viewModel.restablecerDatos { termino = true }
        assertTrue(termino)
        assertEquals(MockData.productos.size, viewModel.productos.value.size)
        assertEquals(CappucareViewModel.MENSAJE_DATOS_RESTABLECIDOS, avisos.last())
    }

    // ---------------------------------------------------------------- repositorio simulado (MockK)

    private fun repositorioSimulado(): FuenteDatos = mockk {
        every { productos } returns flowOf(MockData.productos)
        every { movimientos } returns flowOf(MockData.movimientos)
        coEvery { iniciarSesion(any(), any()) } returns
            com.cappucare.app.data.repository.ResultadoLogin.Exito(MockData.usuarios[0])
    }

    @Test
    fun siNoSePuedenRestablecerLosDatosSeAvisa() = runTest(dispatcher) {
        val fuente = repositorioSimulado()
        coEvery { fuente.restablecerDatos() } throws IllegalStateException("base bloqueada")
        val vm = CappucareViewModel(mockk(relaxed = true), fuente, preferencias)
        val avisos = mensajes(vm)
        var termino = false
        vm.restablecerDatos { termino = true }
        assertTrue(termino)
        assertEquals("No se pudieron restablecer los datos", avisos.single())
    }

    @Test
    fun losErroresDelRepositorioSeMuestranAlUsuario() = runTest(dispatcher) {
        val fuente = repositorioSimulado()
        coEvery { fuente.guardarProducto(any()) } returns ApiResult.SinConexion()
        coEvery { fuente.eliminarProducto("P1") } returns
            ApiResult.Error("El producto ya no existe", tipo = TipoError.NO_ENCONTRADO)
        val vm = CappucareViewModel(mockk(relaxed = true), fuente, preferencias)
        val avisos = mensajes(vm)
        vm.login("encargado@cappucare.cl", "encargado123")
        vm.guardarProducto(nuevoProducto())
        vm.eliminarProducto("P1")
        assertEquals(listOf(ApiResult.MENSAJE_SIN_CONEXION, "El producto ya no existe"), avisos)
        coVerify(exactly = 1) { fuente.guardarProducto(any()) }
        coVerify(exactly = 1) { fuente.eliminarProducto("P1") }
    }
}
