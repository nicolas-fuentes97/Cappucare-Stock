package com.cappucare.app.data.remote

import com.cappucare.app.model.ErroresMovimiento
import com.cappucare.app.model.ErroresProducto

/**
 * Lleva el error de un campo que respondió el servidor (`{error, campo}`) a los mismos objetos
 * de errores de formulario que usa la validación local: así el mensaje aparece bajo el campo
 * correcto (isError + supportingText) igual que cuando la app valida sola.
 */
object ErroresServidor {

    /** Nombre legible de un campo del servidor, para el aviso cuando el formulario no lo muestra. */
    private val etiquetas = mapOf(
        "codigo" to "Código", "nombre" to "Nombre", "descripcion" to "Descripción",
        "categoria" to "Categoría", "unidadMedida" to "Unidad de medida",
        "nivelMinimo" to "Nivel mínimo", "ubicacion" to "Ubicación", "estado" to "Estado",
        "producto" to "Producto", "cantidad" to "Cantidad", "motivo" to "Motivo",
        "tipo" to "Tipo", "observacion" to "Observación", "fecha" to "Fecha"
    )

    /** true si [campo] tiene un lugar en el formulario de producto. */
    fun campoDeProducto(campo: String?): Boolean =
        campo in setOf("codigo", "nombre", "categoria", "unidadMedida", "nivelMinimo", "ubicacion")

    fun campoDeMovimiento(campo: String?): Boolean = campo in setOf("producto", "cantidad", "motivo")

    fun aErroresProducto(campo: String?, mensaje: String) = ErroresProducto(
        codigo = mensaje.takeIf { campo == "codigo" },
        nombre = mensaje.takeIf { campo == "nombre" },
        categoria = mensaje.takeIf { campo == "categoria" },
        unidadMedida = mensaje.takeIf { campo == "unidadMedida" },
        nivelMinimo = mensaje.takeIf { campo == "nivelMinimo" },
        ubicacion = mensaje.takeIf { campo == "ubicacion" }
    )

    fun aErroresMovimiento(campo: String?, mensaje: String) = ErroresMovimiento(
        producto = mensaje.takeIf { campo == "producto" },
        cantidad = mensaje.takeIf { campo == "cantidad" },
        motivo = mensaje.takeIf { campo == "motivo" }
    )

    /** Mensaje para el Snackbar cuando el campo no está en el formulario: "Descripción: El máximo es...". */
    fun mensajeConCampo(campo: String?, mensaje: String): String {
        val etiqueta = campo?.let { etiquetas[it] } ?: return mensaje
        return "$etiqueta: $mensaje"
    }
}
