package com.cappucare.app.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton con la configuración de Retrofit para la API externa (Guía 14).
 * Se crea una sola vez, la primera vez que se usa (by lazy).
 */
object RetrofitInstance {

    /** URL base de la API pública de indicadores económicos (siempre termina en "/"). */
    const val BASE_URL = "https://mindicador.cl/"

    /** Cliente HTTP con tiempos máximos: si la API no responde, la tarjeta muestra el error y "Reintentar". */
    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)                                    // URL base de la API
            .client(cliente)                                      // tiempos máximos de espera
            .addConverterFactory(GsonConverterFactory.create())   // JSON <-> Kotlin
            .build()
            .create(ApiService::class.java)                       // implementa ApiService
    }
}
