package com.cappucare.app.data.remote

/** Tipo de error devuelto por el servidor o detectado al leer su respuesta. */
enum class TipoError {
    SOLICITUD_INVALIDA,   // 400 (y otros 4xx): dato ausente o con formato inválido
    NO_AUTORIZADO,        // 401: credenciales incorrectas o sesión vencida
    SIN_PERMISO,          // 403: el perfil no puede hacer esto (RN-08)
    NO_ENCONTRADO,        // 404
    CONFLICTO,            // 409: por ejemplo código de producto repetido (RN-04)
    REGLA_DE_NEGOCIO,     // 422: stock insuficiente, estado incoherente, etc.
    SERVIDOR,             // 5xx
    RESPUESTA_INVALIDA,   // el servidor respondió algo que la app no entiende
    BLOQUEADO,            // no se pudo establecer una conexión segura (SSL)
    DESCONOCIDO
}

/**
 * Resultado de una llamada a la API. Los tres casos que la interfaz distingue:
 *  - [Success]: llegó la respuesta.
 *  - [Error]: el servidor respondió pero rechazó la operación (mensaje listo para el usuario, y
 *    el [campo] del formulario cuando aplica).
 *  - [SinConexion]: no se pudo llegar al servidor (sin red, servidor apagado, tiempo agotado).
 */
sealed class ApiResult<out T> {

    data class Success<out T>(val datos: T) : ApiResult<T>()

    data class Error(
        val mensaje: String,
        val codigoHttp: Int? = null,
        val campo: String? = null,
        val tipo: TipoError = TipoError.DESCONOCIDO
    ) : ApiResult<Nothing>()

    data class SinConexion(
        val mensaje: String = MENSAJE_SIN_CONEXION,
        val porTiempoAgotado: Boolean = false
    ) : ApiResult<Nothing>()

    val esExito: Boolean get() = this is Success

    fun datosONulo(): T? = (this as? Success)?.datos

    /** Transforma los datos si fue exitoso; los errores pasan tal cual. */
    inline fun <R> map(transformar: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transformar(datos))
        is Error -> this
        is SinConexion -> this
    }

    companion object {
        const val MENSAJE_SIN_CONEXION = "Sin conexión con el servidor. Revisa tu red o la dirección del servidor."
    }
}

/** El servidor respondió, pero faltan campos o traen valores que la app no conoce. */
class RespuestaInvalidaException(mensaje: String) : Exception(mensaje)
