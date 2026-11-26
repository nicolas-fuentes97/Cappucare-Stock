package com.cappucare.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * TODO(E3-3) · Manejo de errores en español.
 *
 * Convierte cualquier falla de una llamada a la API en un [ApiResult] con un mensaje para el
 * usuario, de modo que el repositorio y el ViewModel nunca vean excepciones de red.
 *
 * QUÉ HACER (las tres funciones de este objeto):
 *  1. [llamar]: ejecutar [bloque] y devolver `ApiResult.Success(resultado)`; si falla, devolver
 *     `deExcepcion(e)`. Una `CancellationException` se vuelve a lanzar (nunca se traga).
 *  2. [deCodigoHttp]: convertir un código HTTP y el cuerpo de error (`{"error":"...","campo":"..."}`,
 *     leído con [Gson] hacia [ErrorDto]; si el cuerpo no es JSON, se ignora) en un `ApiResult.Error`:
 *       400 SOLICITUD_INVALIDA · 401 NO_AUTORIZADO · 403 SIN_PERMISO · 404 NO_ENCONTRADO ·
 *       409 CONFLICTO · 422 REGLA_DE_NEGOCIO · 5xx SERVIDOR (siempre [MENSAJE_SERVIDOR]) ·
 *       otro 4xx SOLICITUD_INVALIDA.
 *     El mensaje es el `error` que manda el servidor; si no vino, uno por defecto en español
 *     (401: [MENSAJE_SESION_VENCIDA]). Se conserva `campo` y `codigoHttp`.
 *  3. [deExcepcion]: clasificar la excepción:
 *       - HttpException -> deCodigoHttp(code, errorBody().string()).
 *       - RespuestaInvalidaException, JsonParseException o NullPointerException ->
 *         Error(MENSAJE_RESPUESTA_INVALIDA, tipo RESPUESTA_INVALIDA).
 *       - InterruptedIOException (incluye SocketTimeoutException) ->
 *         SinConexion(MENSAJE_TIEMPO_AGOTADO, porTiempoAgotado = true).
 *       - UnknownHostException, ConnectException, NoRouteToHostException ->
 *         SinConexion(MENSAJE_NO_SE_PUDO_CONECTAR).
 *       - SSLException -> Error(MENSAJE_SEGURIDAD, tipo BLOQUEADO).
 *       - cualquier otra IOException -> SinConexion(MENSAJE_SE_PERDIO_LA_CONEXION).
 *       - cualquier otra cosa -> Error(MENSAJE_INESPERADO, tipo DESCONOCIDO).
 *     (El orden importa: SocketTimeoutException es una InterruptedIOException y esta, una IOException.)
 *
 * CRITERIO DE ACEPTACIÓN: las pruebas de E3-10 (un caso por código 400/401/403/404/409/422/500,
 * cuerpo que no es JSON, tiempo agotado, servidor apagado y respuesta mal formada).
 */
object ManejoErrores {

    private val gson = Gson()

    const val MENSAJE_TIEMPO_AGOTADO = "El servidor tardó demasiado en responder. Intenta de nuevo."
    const val MENSAJE_NO_SE_PUDO_CONECTAR =
        "No se pudo conectar con el servidor. Revisa que esté encendido y que la dirección sea correcta."
    const val MENSAJE_SE_PERDIO_LA_CONEXION = "Se perdió la conexión con el servidor. Intenta de nuevo."
    const val MENSAJE_RESPUESTA_INVALIDA =
        "El servidor respondió algo que la app no entiende. Revisa la dirección del servidor."
    const val MENSAJE_SERVIDOR = "El servidor tuvo un problema. Intenta de nuevo en unos minutos."
    const val MENSAJE_SESION_VENCIDA = "Tu sesión expiró. Ingresa de nuevo."
    const val MENSAJE_SEGURIDAD = "No se pudo establecer una conexión segura con el servidor. Revisa la dirección (https)."
    const val MENSAJE_INESPERADO = "Ocurrió un error inesperado. Intenta de nuevo."

    /** Ejecuta [bloque] (una llamada a ApiService más su mapeo) y traduce las fallas. */
    suspend fun <T> llamar(bloque: suspend () -> T): ApiResult<T> {
        TODO("E3-3: Success(bloque()) y, ante una falla, deExcepcion(e); relanzar CancellationException")
    }

    /** Error HTTP: usa el mensaje que manda el servidor (`{error, campo?}`) cuando existe. */
    fun deCodigoHttp(codigo: Int, cuerpo: String?): ApiResult.Error {
        TODO("E3-3: mapear 400/401/403/404/409/422/5xx a ApiResult.Error con su TipoError")
    }

    /** Clasifica una excepción: error del servidor, falta de conexión o respuesta ilegible. */
    fun deExcepcion(error: Throwable): ApiResult<Nothing> {
        TODO("E3-3: HttpException, respuesta inválida, tiempo agotado, sin conexión, SSL, otro")
    }
}
