package com.cappucare.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/** Solo en el build debug: muestra en Logcat cada petición y respuesta (sin el token). */
object RegistroHttp {
    fun agregar(builder: OkHttpClient.Builder) {
        val registro = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
        builder.addInterceptor(registro)
    }
}
