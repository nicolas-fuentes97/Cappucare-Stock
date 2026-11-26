package com.cappucare.app.model

enum class TipoMovimiento(val etiqueta: String) {
    INGRESO("Ingreso (recepción de mercadería)"),
    SALIDA("Salida")
}

/**
 * Motivo de una salida. Regla de negocio del caso (RN-02): una salida por venta se registra con
 * motivo VENTA. Los demás motivos son una propuesta pendiente de confirmar con el CITT.
 */
enum class MotivoSalida(val etiqueta: String) {
    VENTA("Venta"),
    VENCIMIENTO("Vencimiento"),
    MERMA("Merma o daño"),
    AJUSTE("Ajuste de inventario"),
    OTRO("Otro")
}

/**
 * Movimiento de inventario (entidad "Movimiento de inventario" del caso).
 *
 * Se guarda en la tabla "movimientos" (MovimientoEntity), relacionada con "productos" por
 * productoId (clave foránea). RN-06: una vez registrado no se edita ni se elimina.
 *
 * RF-06: [responsable] es el nombre del usuario que registró el movimiento y
 * [responsablePerfil] su perfil en ese momento. Ambos los asigna el ViewModel desde la sesión.
 */
data class Movimiento(
    val id: String,
    val productoId: String,
    val fecha: String,
    val tipo: TipoMovimiento,
    val cantidad: Int,
    val motivo: MotivoSalida? = null,
    val observacion: String = "",
    val responsable: String,
    val responsablePerfil: Perfil
)

/** Errores de validación del formulario de movimiento (null = sin error). */
data class ErroresMovimiento(
    val producto: String? = null,
    val cantidad: String? = null,
    val motivo: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(producto, cantidad, motivo).any { it != null }

    /** Primer mensaje de error, para avisar en un solo texto (por ejemplo en un Snackbar). */
    val primerMensaje: String?
        get() = listOf(producto, cantidad, motivo).firstOrNull { it != null }
}
