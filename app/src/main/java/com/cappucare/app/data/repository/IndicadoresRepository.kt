package com.cappucare.app.data.repository

import com.cappucare.app.data.remote.ApiService
import com.cappucare.app.data.remote.IndicadorDto
import com.cappucare.app.data.remote.IndicadoresDto
import com.cappucare.app.data.remote.RetrofitInstance
import com.cappucare.app.model.Indicador

/**
 * Repositorio de la API externa: el ViewModel solo lo conoce a él, no a Retrofit.
 * Recibe el [ApiService] por el constructor: en la app es el de RetrofitInstance y en las pruebas
 * un doble de MockK.
 */
class IndicadoresRepository(private val api: ApiService = RetrofitInstance.api) {

    /** Pide los indicadores del día y los convierte al modelo de la app (dólar, euro, UF y UTM). */
    suspend fun obtenerIndicadores(): List<Indicador> = api.getIndicadores().aIndicadores()
}

/** Orden en que se muestran los indicadores en el Panel. */
internal fun IndicadoresDto.aIndicadores(): List<Indicador> =
    listOfNotNull(dolar, euro, uf, utm).mapNotNull { it.aIndicador() }

/** DTO -> modelo. Si falta el código o el valor, el indicador se descarta (no se inventan datos). */
internal fun IndicadorDto.aIndicador(): Indicador? {
    val codigoValido = codigo?.takeIf { it.isNotBlank() } ?: return null
    val valorValido = valor ?: return null
    return Indicador(
        codigo = codigoValido,
        nombre = nombre?.takeIf { it.isNotBlank() } ?: codigoValido.uppercase(),
        valor = valorValido,
        unidad = unidadMedida.orEmpty(),
        fecha = fecha.orEmpty().take(10)          // "2026-11-25T03:00:00.000Z" -> "2026-11-25"
    )
}
