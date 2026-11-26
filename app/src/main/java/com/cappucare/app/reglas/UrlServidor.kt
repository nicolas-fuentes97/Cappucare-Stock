package com.cappucare.app.reglas

/** Resultado de revisar la dirección del servidor que escribió el usuario. */
sealed class ResultadoUrl {
    /** [url] ya normalizada: con esquema, sin espacios y terminada en "/" (lo exige Retrofit). */
    data class Valida(val url: String) : ResultadoUrl()
    data class Invalida(val mensaje: String) : ResultadoUrl()
}

/**
 * Validación y normalización de la dirección del servidor (API). Kotlin puro: se prueba con JUnit
 * (E3-10, UrlServidorTest) sin emulador.
 *
 * Política de seguridad: https se acepta hacia cualquier servidor; http (texto plano) solo hacia
 * el propio equipo o la red local: 10.0.2.2 (emulador), localhost, 127.x, 10.x, 172.16-31.x,
 * 192.168.x.x y nombres de red local (.local o sin puntos). Android no puede expresar rangos de
 * IP en network_security_config.xml, por eso la regla se aplica aquí.
 */
object UrlServidor {

    const val URL_EMULADOR = "http://10.0.2.2:3000/"

    /**
     * TODO(E3-8a) · Revisa el texto escrito por el usuario y devuelve la dirección normalizada
     * ([ResultadoUrl.Valida]) o el motivo del rechazo ([ResultadoUrl.Invalida], en español).
     *
     * QUÉ HACER, en este orden:
     *  1. Recortar espacios. Vacía -> "Ingresa la dirección del servidor". Más de 200 caracteres ->
     *     "La dirección es demasiado larga". Con espacios en medio -> "La dirección no puede tener espacios".
     *  2. Sin esquema (no empieza con `algo://`): anteponer `http://` si el host es privado
     *     ([esHostPrivado]) y `https://` si no (ej.: `192.168.1.20:3000` -> `http://192.168.1.20:3000`).
     *  3. Parsear con `java.net.URI`. Si falla o no tiene host -> "La dirección no es válida.
     *     Ejemplo: http://192.168.1.20:3000/". Esquema distinto de http/https -> "Usa una dirección
     *     que empiece con http:// o https://".
     *  4. Rechazar usuario/clave en la URL, parámetros (`?`) o `#`, y puertos fuera de 1..65535.
     *  5. `http` hacia un host NO privado -> rechazar con un mensaje que indique usar https.
     *  6. Devolver `esquema://host[:puerto][/ruta]/` en minúsculas y con UNA barra final.
     *
     * CRITERIO DE ACEPTACIÓN: `normalizar("192.168.1.20:3000")` = Valida("http://192.168.1.20:3000/");
     * `normalizar("http://midominio.com")` = Invalida; `normalizar("https://midominio.com/api")` =
     * Valida("https://midominio.com/api/").
     */
    fun normalizar(texto: String): ResultadoUrl {
        TODO("E3-8a: validar y normalizar la dirección del servidor (ver reglas arriba)")
    }

    /** Mensaje de error de la dirección o null si es válida (para mostrarlo bajo el campo). */
    fun error(texto: String): String? = (normalizar(texto) as? ResultadoUrl.Invalida)?.mensaje

    /**
     * (Ya resuelta.) true si [host] es el propio equipo o una red local: se puede usar con http.
     * Acepta el host tal cual lo entrega la URL (IPv6 con o sin corchetes).
     */
    fun esHostPrivado(host: String): Boolean {
        val h = host.trim().lowercase().removePrefix("[").removeSuffix("]")
        if (h.isEmpty()) return false
        if (h == "localhost" || h == "::1" || h == "0:0:0:0:0:0:0:1") return true
        if (h.startsWith("fe80:") || h.startsWith("fc") && h.contains(':') || h.startsWith("fd") && h.contains(':')) return true
        val octetos = h.split('.')
        if (octetos.size == 4 && octetos.all { it.isNotEmpty() && it.all(Char::isDigit) && it.length <= 3 }) {
            val n = octetos.map { it.toInt() }
            if (n.any { it > 255 }) return false
            return n[0] == 10 ||
                n[0] == 127 ||
                (n[0] == 172 && n[1] in 16..31) ||
                (n[0] == 192 && n[1] == 168) ||
                (n[0] == 169 && n[1] == 254)
        }
        // Nombres de red local: "mi-pc.local" o un nombre sin puntos ("mi-pc").
        return h.endsWith(".local") || (!h.contains('.') && !h.contains(':'))
    }
}
