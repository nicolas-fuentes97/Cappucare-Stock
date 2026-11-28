package com.cappucare.app.data.remote

import com.google.gson.annotations.SerializedName

/*
 * DTOs: la forma EXACTA del JSON que responde https://mindicador.cl/api (Gson lo convierte en
 * estos objetos). Solo se declaran los indicadores que usa la app; Gson ignora el resto.
 * Los campos son anulables: si la API omitiera alguno, el mapeo lo descarta en vez de cerrar la app.
 *
 * Ejemplo de respuesta (resumida):
 * {
 *   "version": "1.7.0", "autor": "mindicador.cl", "fecha": "2026-11-25T03:00:00.000Z",
 *   "dolar": { "codigo": "dolar", "nombre": "Dólar observado", "unidad_medida": "Pesos",
 *              "fecha": "2026-11-25T03:00:00.000Z", "valor": 950.12 },
 *   "euro": {...}, "uf": {...}, "utm": {...}, ...
 * }
 */

data class IndicadoresDto(
    val fecha: String?,
    val dolar: IndicadorDto?,
    val euro: IndicadorDto?,
    val uf: IndicadorDto?,
    val utm: IndicadorDto?
)

data class IndicadorDto(
    val codigo: String?,
    val nombre: String?,
    @SerializedName("unidad_medida") val unidadMedida: String?,
    val fecha: String?,
    val valor: Double?
)
