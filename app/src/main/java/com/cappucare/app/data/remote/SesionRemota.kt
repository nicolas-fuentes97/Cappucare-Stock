package com.cappucare.app.data.remote

/**
 * Sesión con el servidor: el token que se agrega a cada petición (InterceptorToken) y el nombre
 * de quien la abrió. Vive solo en memoria: al cerrar la app hay que ingresar de nuevo. Un token
 * nulo con un usuario activo significa que se ingresó sin conexión (solo datos guardados).
 */
class SesionRemota {
    @Volatile
    var token: String? = null
        private set

    @Volatile
    var nombreUsuario: String? = null
        private set

    val hayToken: Boolean get() = token != null

    fun abrir(token: String?, nombreUsuario: String) {
        this.token = token
        this.nombreUsuario = nombreUsuario
    }

    /** Cierra la sesión solo si sigue siendo la misma (evita cerrar una sesión nueva por una demora). */
    @Synchronized
    fun cerrar(tokenEsperado: String?) {
        if (token == tokenEsperado) {
            token = null
            nombreUsuario = null
        }
    }
}
