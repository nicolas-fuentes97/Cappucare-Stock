package com.cappucare.app.model

/**
 * Perfiles definidos en el caso (actores).
 *
 * RN-08 (supuesto SUP-06, pendiente de confirmar con el CITT): solo el Personal encargado crea y
 * edita productos y cambia su estado; el Usuario autorizado consulta y registra movimientos.
 * Si el CITT define otros permisos, se cambia aquí y todas las pantallas se ajustan solas.
 */
enum class Perfil(val etiqueta: String, val puedeEditarCatalogo: Boolean) {
    ENCARGADO("Personal encargado", puedeEditarCatalogo = true),
    USUARIO_AUTORIZADO("Usuario autorizado", puedeEditarCatalogo = false)
}
