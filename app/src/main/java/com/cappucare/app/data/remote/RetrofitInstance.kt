package com.cappucare.app.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * TODO(E3-2) · Cliente HTTP y servicio Retrofit.
 *
 * QUÉ HACER:
 *  1. [crearCliente]: construir un `OkHttpClient` con
 *       - tiempo de conexión 8 s ([SEGUNDOS_CONEXION]), lectura y escritura 15 s
 *         ([segundosLectura]) y tiempo total por llamada 25 s ([segundosTotal]);
 *       - el [InterceptorToken] (E3-1) alimentado con [token];
 *       - en el build debug, el registro HTTP: `RegistroHttp.agregar(builder)` (ya existe en
 *         src/debug y src/release; en release no hace nada).
 *  2. [crearServicio]: `Retrofit.Builder()` con `baseUrl(urlBase)`, ese cliente y
 *     `GsonConverterFactory`, y devolver `create(ApiService::class.java)`.
 *
 * CRITERIO DE ACEPTACIÓN: `crearServicio("http://10.0.2.2:3000/") { null }.health()` llega a
 * `GET /health` del servidor; un servidor que tarda más que [segundosLectura] termina en
 * `SocketTimeoutException` (la prueba E3-9 lo comprueba con MockWebServer y tiempos cortos).
 *
 * [urlBase] debe terminar en "/" (UrlServidor.normalizar de E3-8 lo garantiza).
 */
object RetrofitInstance {

    const val SEGUNDOS_CONEXION = 8L
    const val SEGUNDOS_LECTURA = 15L
    const val SEGUNDOS_TOTAL = 25L

    /** [token] se consulta en cada petición. Los tiempos se pueden acortar en las pruebas. */
    fun crearCliente(
        token: () -> String?,
        segundosLectura: Long = SEGUNDOS_LECTURA,
        segundosTotal: Long = SEGUNDOS_TOTAL
    ): OkHttpClient {
        TODO("E3-2: OkHttpClient con timeouts, InterceptorToken y RegistroHttp.agregar(builder)")
    }

    fun crearServicio(
        urlBase: String,
        segundosLectura: Long = SEGUNDOS_LECTURA,
        segundosTotal: Long = SEGUNDOS_TOTAL,
        token: () -> String?
    ): ApiService {
        TODO("E3-2: Retrofit.Builder().baseUrl(urlBase).client(...).addConverterFactory(Gson).build().create(...)")
    }
}

/**
 * (Ya resuelta, no se modifica.) Entrega el [ApiService] para la dirección actual del servidor y
 * lo reutiliza mientras no cambie: cambiar la dirección desde la app no necesita recompilar.
 */
class FabricaApi(private val sesion: SesionRemota) {
    private var urlActual: String? = null
    private var servicio: ApiService? = null

    @Synchronized
    fun servicio(urlBase: String): ApiService {
        val existente = servicio
        if (existente != null && urlActual == urlBase) return existente
        return RetrofitInstance.crearServicio(urlBase) { sesion.token }.also {
            servicio = it
            urlActual = urlBase
        }
    }
}
