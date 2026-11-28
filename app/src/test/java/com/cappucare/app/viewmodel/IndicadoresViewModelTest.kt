package com.cappucare.app.viewmodel

import com.cappucare.app.data.repository.IndicadoresRepository
import com.cappucare.app.model.Indicador
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * API externa: pruebas del ViewModel de indicadores (mismo enfoque de la Guía 15).
 * Se prueba el ViewModel REAL; lo único simulado (MockK) es el repositorio, así no se usa internet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IndicadoresViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val dolar = Indicador("dolar", "Dólar observado", 950.12, "Pesos", "2026-11-25")
    private val uf = Indicador("uf", "Unidad de fomento (UF)", 39700.5, "Pesos", "2026-11-25")

    @Before
    fun preparar() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    @Test
    fun alIniciarPasaDeCargandoAExitoConLosIndicadores() = runTest(dispatcher) {
        val repo = mockk<IndicadoresRepository>()
        coEvery { repo.obtenerIndicadores() } returns listOf(dolar, uf)

        val viewModel = IndicadoresViewModel(repo)
        // La corrutina aún no se ejecuta: el estado sigue en Cargando
        assertEquals(IndicadoresUiState.Cargando, viewModel.uiState.value)

        advanceUntilIdle()   // ejecuta las corrutinas pendientes
        assertEquals(IndicadoresUiState.Exito(listOf(dolar, uf)), viewModel.uiState.value)
    }

    @Test
    fun sinInternetElEstadoEsErrorConUnMensajeParaElUsuario() = runTest(dispatcher) {
        val repo = mockk<IndicadoresRepository>()
        coEvery { repo.obtenerIndicadores() } throws IOException("sin red")

        val viewModel = IndicadoresViewModel(repo)
        advanceUntilIdle()

        assertEquals(IndicadoresUiState.Error(IndicadoresViewModel.MENSAJE_SIN_CONEXION), viewModel.uiState.value)
    }

    @Test
    fun siLaApiRespondeConErrorHttpSeMuestraElCodigo() = runTest(dispatcher) {
        val repo = mockk<IndicadoresRepository>()
        coEvery { repo.obtenerIndicadores() } throws
            HttpException(Response.error<Any>(503, "".toResponseBody(null)))

        val viewModel = IndicadoresViewModel(repo)
        advanceUntilIdle()

        val estado = viewModel.uiState.value as IndicadoresUiState.Error
        assertTrue(estado.mensaje.contains("503"))
    }

    @Test
    fun siLaRespuestaNoSePuedeLeerSeMuestraUnErrorGenerico() = runTest(dispatcher) {
        val repo = mockk<IndicadoresRepository>()
        coEvery { repo.obtenerIndicadores() } throws IllegalStateException("JSON mal formado")

        val viewModel = IndicadoresViewModel(repo)
        advanceUntilIdle()

        assertEquals(IndicadoresUiState.Error(IndicadoresViewModel.MENSAJE_RESPUESTA_INVALIDA), viewModel.uiState.value)
    }

    @Test
    fun unaListaVaciaSeMuestraComoError() = runTest(dispatcher) {
        val repo = mockk<IndicadoresRepository>()
        coEvery { repo.obtenerIndicadores() } returns emptyList()

        val viewModel = IndicadoresViewModel(repo)
        advanceUntilIdle()

        assertEquals(IndicadoresUiState.Error(IndicadoresViewModel.MENSAJE_SIN_DATOS), viewModel.uiState.value)
    }

    @Test
    fun reintentarDespuesDeUnErrorCargaLosIndicadores() = runTest(dispatcher) {
        val repo = mockk<IndicadoresRepository>()
        coEvery { repo.obtenerIndicadores() } throws IOException("sin red") andThen listOf(dolar)

        val viewModel = IndicadoresViewModel(repo)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is IndicadoresUiState.Error)

        viewModel.cargarIndicadores()     // botón "Reintentar"
        advanceUntilIdle()

        assertEquals(IndicadoresUiState.Exito(listOf(dolar)), viewModel.uiState.value)
        coVerify(exactly = 2) { repo.obtenerIndicadores() }
    }
}
