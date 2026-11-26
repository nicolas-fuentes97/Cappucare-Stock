package com.cappucare.app.reglas

import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.Producto

/**
 * RF-09 · Identificar productos bajo su nivel mínimo.
 * RN-07 · Definición de "bajo el nivel mínimo".
 *
 * Regla de negocio PURA (sin Android ni Compose): por eso se puede probar con JUnit sin
 * emulador (ver app/src/test/.../ReglaReposicionTest.kt). El ViewModel la usa para la lista
 * de alertas, el contador del Panel, la marca en Productos/Detalle y la notificación, así
 * todas las pantallas aplican EXACTAMENTE la misma comparación (RN-07, punto 1).
 *
 * Decisiones de diseño:
 *  - "Bajo el mínimo" es MENOR ESTRICTO: disponible < nivelMinimo.
 *    Si disponible == nivelMinimo todavía no se alerta (P5: 6 de 5 no aparece).
 *  - Los productos DADO_DE_BAJA no se consideran (ya no se reponen).
 *  - Orden de las alertas: primero el que tiene mayor falta (nivelMinimo - disponible); si
 *    empatan, por nombre.
 *  - Supuesto SUP-07 (pendiente con el CITT): el nivel mínimo es fijo por producto.
 */
object ReglaReposicion {

    /** RN-07: true si el producto necesita reposición. */
    fun estaBajoMinimo(producto: Producto): Boolean =
        producto.estado != EstadoProducto.DADO_DE_BAJA &&
            producto.cantidadDisponible < producto.nivelMinimo

    /** Unidades que faltan para llegar al nivel mínimo (nunca negativo). */
    fun faltaParaMinimo(producto: Producto): Int =
        (producto.nivelMinimo - producto.cantidadDisponible).coerceAtLeast(0)

    /** Fracción disponible / mínimo, entre 0 y 1 (para la barra de la pantalla de Reposición). */
    fun nivelDeCobertura(producto: Producto): Float =
        if (producto.nivelMinimo <= 0) 1f
        else (producto.cantidadDisponible.toFloat() / producto.nivelMinimo).coerceIn(0f, 1f)

    /** RF-09: productos bajo su mínimo, del más urgente al menos urgente (desempate por nombre). */
    fun filtrarBajoMinimo(productos: List<Producto>): List<Producto> =
        productos
            .filter { estaBajoMinimo(it) }
            .sortedWith(
                compareByDescending<Producto> { faltaParaMinimo(it) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.nombre }
            )

    /**
     * RNF-07 (opcional): true si un movimiento hizo que el producto PASARA de estar al día a
     * estar bajo el mínimo. Un producto que ya estaba bajo el mínimo no vuelve a avisar: llega
     * una notificación por cruce, no una por cada movimiento.
     */
    fun cruzoElMinimo(antes: Producto, despues: Producto): Boolean =
        !estaBajoMinimo(antes) && estaBajoMinimo(despues)
}
