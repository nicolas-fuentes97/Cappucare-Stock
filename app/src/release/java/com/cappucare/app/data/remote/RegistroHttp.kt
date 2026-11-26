package com.cappucare.app.data.remote

import okhttp3.OkHttpClient

/** Build release: no se registra el tráfico HTTP (la dependencia de registro solo existe en debug). */
object RegistroHttp {
    @Suppress("UNUSED_PARAMETER")
    fun agregar(builder: OkHttpClient.Builder) = Unit
}
