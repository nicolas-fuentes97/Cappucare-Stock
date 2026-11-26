package com.cappucare.app.model

/**
 * Usuario que inicia sesión en la app. Su nombre y perfil quedan registrados como responsable
 * de cada movimiento de inventario (RF-06).
 *
 * La clave nunca se guarda en texto plano: [claveHash] es el resumen SHA-256 con sal
 * (ver reglas/Seguridad.kt). Solo datos ficticios (ver MockData.usuarios).
 */
data class Usuario(
    val id: String,
    val nombre: String,
    val usuario: String,
    val claveHash: String,
    val perfil: Perfil
)

/** Errores de validación del login (un campo de error por campo; null = sin error). */
data class ErroresLogin(
    val usuario: String? = null,
    val clave: String? = null
) {
    val hayErrores: Boolean
        get() = usuario != null || clave != null
}
