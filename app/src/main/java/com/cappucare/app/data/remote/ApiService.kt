package com.cappucare.app.data.remote

import retrofit2.http.GET

/**
 * API EXTERNA (Guía 14): esta interfaz define los endpoints HTTP y Retrofit escribe la
 * implementación. Es una API pública de terceros (mindicador.cl): la app solo LEE sus datos.
 * Los productos y movimientos NO pasan por aquí: esos se guardan en Room (FuenteLocal).
 */
interface ApiService {

    /** GET https://mindicador.cl/api -> indicadores económicos del día (200 OK). */
    @GET("api")
    suspend fun getIndicadores(): IndicadoresDto
}
