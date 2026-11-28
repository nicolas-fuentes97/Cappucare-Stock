package com.cappucare.app.repository

import com.cappucare.app.data.remote.ApiService
import com.cappucare.app.data.remote.IndicadorDto
import com.cappucare.app.data.remote.IndicadoresDto
import com.cappucare.app.data.repository.IndicadoresRepository
import com.cappucare.app.data.repository.aIndicador
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * API externa: el repositorio llama a la API correcta y convierte el JSON (DTO) al modelo.
 * La API se simula con MockK: la prueba no usa internet.
 */
class IndicadoresRepositoryTest {

    private fun dto(codigo: String?, valor: Double?, nombre: String? = "Nombre $codigo") =
        IndicadorDto(codigo, nombre, "Pesos", "2026-11-25T03:00:00.000Z", valor)

    @Test
    fun obtenerIndicadoresLlamaALaApiYDevuelveDolarEuroUfYUtmEnEseOrden() = runTest {
        val api = mockk<ApiService>()
        coEvery { api.getIndicadores() } returns IndicadoresDto(
            fecha = "2026-11-25T03:00:00.000Z",
            dolar = dto("dolar", 950.12),
            euro = dto("euro", 1010.5),
            uf = dto("uf", 39700.5),
            utm = dto("utm", 68000.0)
        )

        val indicadores = IndicadoresRepository(api).obtenerIndicadores()

        assertEquals(listOf("dolar", "euro", "uf", "utm"), indicadores.map { it.codigo })
        assertEquals(950.12, indicadores.first().valor, 0.0001)
        assertEquals("2026-11-25", indicadores.first().fecha)
        coVerify(exactly = 1) { api.getIndicadores() }
    }

    @Test
    fun losIndicadoresQueFaltanOVienenIncompletosSeDescartan() = runTest {
        val api = mockk<ApiService>()
        coEvery { api.getIndicadores() } returns IndicadoresDto(
            fecha = null,
            dolar = dto("dolar", null),        // sin valor
            euro = null,                       // no vino
            uf = dto("", 39700.5),             // sin código
            utm = dto("utm", 68000.0)
        )

        val indicadores = IndicadoresRepository(api).obtenerIndicadores()

        assertEquals(listOf("utm"), indicadores.map { it.codigo })
    }

    @Test
    fun sinNombreSeUsaElCodigoEnMayusculasYSinFechaQuedaVacia() {
        val indicador = IndicadorDto("utm", "", null, null, 1.0).aIndicador()!!
        assertEquals("UTM", indicador.nombre)
        assertEquals("", indicador.fecha)
        assertEquals("", indicador.unidad)
        assertNull(IndicadorDto(null, "x", "Pesos", null, 1.0).aIndicador())
        assertTrue(IndicadorDto("uf", null, "Pesos", null, 2.0).aIndicador()!!.nombre == "UF")
    }
}
