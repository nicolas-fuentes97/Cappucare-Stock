package com.cappucare.app.reglas

import java.security.MessageDigest

/**
 * RNF-08 · Acceso seguro: las claves no se guardan en texto plano.
 *
 * Se guarda el resumen SHA-256 de "SAL:clave" en hexadecimal. La sal es fija y pública porque
 * este prototipo solo maneja usuarios ficticios (RNF-04).
 *
 * Limitación conocida: en un sistema real se usaría un algoritmo lento (PBKDF2, bcrypt o
 * Argon2) con una sal distinta por usuario y la verificación se haría en un servidor.
 *
 * Kotlin puro (sin Android): se prueba con JUnit en SeguridadTest.
 */
object Seguridad {

    private const val SAL = "cappucare-stock-demo-v1"

    /** Resumen SHA-256 (64 caracteres hexadecimales). La misma clave siempre da el mismo resumen. */
    fun hash(clave: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$SAL:$clave".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** true si [clave] corresponde al [hashGuardado]. Compara en tiempo constante. */
    fun verificar(clave: String, hashGuardado: String): Boolean =
        MessageDigest.isEqual(
            hash(clave).toByteArray(Charsets.UTF_8),
            hashGuardado.toByteArray(Charsets.UTF_8)
        )
}
