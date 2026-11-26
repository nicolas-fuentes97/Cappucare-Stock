package com.cappucare.app.reglas

import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reglas de negocio del inventario, en Kotlin puro (sin Android): se prueban con JUnit en
 * ReglasInventarioTest. Las usan el ViewModel y el Repository.
 */
object ReglasInventario {

    /**
     * RF-07 · Nueva cantidad disponible después de un movimiento.
     *  - RN-01: ingreso suma, salida resta (la cantidad no se edita a mano).
     *  - RN-03: el stock nunca queda negativo → lanza IllegalArgumentException("Solo hay N disponibles").
     *  - Decisión de diseño: si llega a 0 y estaba DISPONIBLE pasa a AGOTADO; si vuelve a tener
     *    unidades y estaba AGOTADO pasa a DISPONIBLE. VENCIDO y DADO_DE_BAJA no cambian solos.
     */
    fun aplicarMovimiento(producto: Producto, tipo: TipoMovimiento, cantidad: Int): Producto {
        require(cantidad > 0) { "Ingresa una cantidad mayor que 0" }
        val nuevaCantidad = when (tipo) {
            TipoMovimiento.INGRESO -> producto.cantidadDisponible + cantidad
            TipoMovimiento.SALIDA -> producto.cantidadDisponible - cantidad
        }
        require(nuevaCantidad >= 0) { "Solo hay ${producto.cantidadDisponible} disponibles" }

        val nuevoEstado = when {
            nuevaCantidad == 0 && producto.estado == EstadoProducto.DISPONIBLE -> EstadoProducto.AGOTADO
            nuevaCantidad > 0 && producto.estado == EstadoProducto.AGOTADO -> EstadoProducto.DISPONIBLE
            else -> producto.estado
        }
        return producto.copy(cantidadDisponible = nuevaCantidad, estado = nuevoEstado)
    }

    /**
     * RF-03 · Valida un cambio manual de estado. Devuelve el mensaje de error o null si se puede.
     *  - Mismo estado: no se hace nada (se avisa).
     *  - AGOTADO solo con 0 unidades; DISPONIBLE solo con al menos 1 unidad (el estado debe ser
     *    coherente con la cantidad, que solo cambia con movimientos).
     */
    fun validarCambioEstado(producto: Producto, nuevoEstado: EstadoProducto): String? = when {
        producto.estado == nuevoEstado -> "Sin cambios: el producto ya está en estado ${nuevoEstado.etiqueta}"
        nuevoEstado == EstadoProducto.AGOTADO && producto.cantidadDisponible > 0 ->
            "Solo un producto sin unidades puede quedar agotado (hay ${producto.cantidadDisponible})"
        nuevoEstado == EstadoProducto.DISPONIBLE && producto.cantidadDisponible == 0 ->
            "Un producto sin unidades no puede quedar disponible: registra un ingreso"
        else -> null
    }

    /**
     * RF-08 + RF-02 · Búsqueda por nombre o código (sin distinguir mayúsculas, ignorando espacios
     * al inicio y al final; texto vacío = todos) y filtro por categoría (null = todas).
     */
    fun filtrarProductos(productos: List<Producto>, texto: String, categoria: CategoriaInsumo?): List<Producto> {
        val buscado = texto.trim()
        return productos.filter { producto ->
            val coincideTexto = buscado.isEmpty() ||
                producto.nombre.contains(buscado, ignoreCase = true) ||
                producto.codigo.contains(buscado, ignoreCase = true)
            val coincideCategoria = categoria == null || producto.categoria == categoria
            coincideTexto && coincideCategoria
        }
    }

    /** RNF-06 · Producto con exactamente ese código (QR o código de barras escaneado). RN-04 garantiza uno solo. */
    fun buscarPorCodigo(productos: List<Producto>, codigo: String): Producto? =
        productos.find { it.codigo.equals(codigo.trim(), ignoreCase = true) }

    /** RN-05 · Fecha y hora actuales en formato ordenable. */
    fun fechaActual(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}
