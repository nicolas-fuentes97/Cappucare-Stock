package com.cappucare.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * TODO(E3-1) · Interceptor del token.
 *
 * QUÉ HACER: en [intercept], agregar el encabezado `Authorization: Bearer <token>` a cada
 * petición cuando [token] devuelve un valor no vacío. NO se agrega en `auth/login` ni en `health`
 * (esas rutas son públicas). El token se consulta en CADA petición (llamando a `token()`), no se
 * guarda en una variable: así un cambio de sesión se aplica de inmediato.
 *
 * CRITERIO DE ACEPTACIÓN: con una sesión abierta, una petición a `productos` sale con el
 * encabezado `Authorization: Bearer abc`; sin sesión, o hacia `auth/login` o `health`, sale sin
 * encabezado (lo verifica la prueba E3-9 con MockWebServer: `takeRequest().getHeader(...)`).
 *
 * Pista: `chain.request().newBuilder().header(...).build()` y `chain.proceed(...)`.
 */
class InterceptorToken(private val token: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        TODO("E3-1: agregar Authorization: Bearer <token> salvo en auth/login y health")
    }
}
