package com.cappucare.app.data.remote

/** Resultado de "Probar conexión". */
data class ResultadoPruebaServidor(val exito: Boolean, val mensaje: String, val urlNormalizada: String? = null)

/**
 * TODO(E3-8b) · "Probar conexión" del diálogo Servidor.
 *
 * QUÉ HACER en [probar]:
 *  1. Validar [texto] con `UrlServidor.normalizar` (E3-8a). Si es inválida, devolver
 *     `ResultadoPruebaServidor(false, <mensaje de la validación>)` sin llamar a la red.
 *  2. Si es válida, llamar a `GET /health` con `ManejoErrores.llamar { crearServicio(url).health() }`:
 *       - Success con `status == "ok"` -> `ResultadoPruebaServidor(true, "Conexión correcta (versión X)", url)`
 *         (sin la versión si el servidor no la informa);
 *       - Success con otro status -> exito = false con `ManejoErrores.MENSAJE_RESPUESTA_INVALIDA`;
 *       - Error o SinConexion -> exito = false con el mensaje que trae el resultado.
 *
 * CRITERIO DE ACEPTACIÓN: servidor encendido -> "Conexión correcta"; servidor apagado -> el
 * mensaje de sin conexión; dirección mal escrita -> el mensaje de validación (sin tocar la red).
 */
object PruebaServidor {

    suspend fun probar(texto: String, crearServicio: (String) -> ApiService): ResultadoPruebaServidor {
        TODO("E3-8b: normalizar la URL, llamar a /health y traducir el resultado")
    }
}
