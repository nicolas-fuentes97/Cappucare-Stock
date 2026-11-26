package com.cappucare.app.reglas

import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento

/** Un movimiento junto con la cantidad que le quedó al producto justo después de él. */
data class MovimientoConSaldo(val movimiento: Movimiento, val saldo: Int)

/** Totales de unidades ingresadas y retiradas en un conjunto de movimientos. */
data class ResumenHistorial(val totalIngresos: Int, val totalSalidas: Int)

/**
 * RF-10 · Historial comprensible de ingresos y salidas. Kotlin puro: se prueba con JUnit en
 * ReglasHistorialTest.
 */
object ReglasHistorial {

    /** Variación de stock que produce un movimiento: + en ingresos, − en salidas. */
    private fun variacion(m: Movimiento): Int =
        if (m.tipo == TipoMovimiento.INGRESO) m.cantidad else -m.cantidad

    /**
     * Movimientos de un producto (null = todos), del más reciente al más antiguo. La fecha
     * "yyyy-MM-dd HH:mm" se ordena correctamente como texto; sortedByDescending es estable, así
     * que si dos tienen el mismo minuto se respeta el orden de entrada (Room entrega primero el
     * último registrado).
     */
    fun historial(movimientos: List<Movimiento>, productoId: String?): List<Movimiento> =
        movimientos
            .filter { productoId == null || it.productoId == productoId }
            .sortedByDescending { it.fecha }

    /**
     * Agrega a cada movimiento el saldo del producto después de él. El saldo se calcula hacia
     * atrás desde la cantidad disponible actual de cada producto, así el movimiento más reciente
     * siempre termina en la cantidad real y no depende del filtro que se aplique después.
     * [tipo] (opcional) deja solo ingresos o solo salidas, sin alterar los saldos.
     */
    fun conSaldo(
        movimientos: List<Movimiento>,
        productos: List<Producto>,
        productoId: String?,
        tipo: TipoMovimiento? = null
    ): List<MovimientoConSaldo> {
        val saldoActual = productos.associate { it.id to it.cantidadDisponible }.toMutableMap()
        return historial(movimientos, null)
            .map { movimiento ->
                val saldo = saldoActual[movimiento.productoId] ?: 0
                saldoActual[movimiento.productoId] = saldo - variacion(movimiento)
                MovimientoConSaldo(movimiento, saldo)
            }
            .filter { productoId == null || it.movimiento.productoId == productoId }
            .filter { tipo == null || it.movimiento.tipo == tipo }
    }

    /** Totales de ingresos y salidas de un producto (null = todos). */
    fun resumen(movimientos: List<Movimiento>, productoId: String?): ResumenHistorial {
        val lista = movimientos.filter { productoId == null || it.productoId == productoId }
        return ResumenHistorial(
            totalIngresos = lista.filter { it.tipo == TipoMovimiento.INGRESO }.sumOf { it.cantidad },
            totalSalidas = lista.filter { it.tipo == TipoMovimiento.SALIDA }.sumOf { it.cantidad }
        )
    }

    /**
     * Texto CSV (separador ";", que abre bien en Excel con configuración regional de Chile) de
     * los movimientos entregados. Recurso nativo: se guarda con el selector de archivos del
     * sistema (SAF) desde HistorialMovimientosScreen.
     */
    fun aCsv(items: List<MovimientoConSaldo>, productos: List<Producto>): String {
        val porId = productos.associateBy { it.id }
        val encabezado = listOf(
            "Fecha", "Código", "Producto", "Tipo", "Cantidad", "Motivo", "Saldo", "Responsable", "Perfil", "Observación"
        )
        val filas = items.map { (m, saldo) ->
            val producto = porId[m.productoId]
            listOf(
                m.fecha,
                producto?.codigo ?: m.productoId,
                producto?.nombre.orEmpty(),
                if (m.tipo == TipoMovimiento.INGRESO) "Ingreso" else "Salida",
                m.cantidad.toString(),
                m.motivo?.etiqueta.orEmpty(),
                saldo.toString(),
                m.responsable,
                m.responsablePerfil.etiqueta,
                m.observacion
            )
        }
        return (listOf(encabezado) + filas).joinToString("\r\n") { fila -> fila.joinToString(";") { celdaCsv(it) } } + "\r\n"
    }

    /** Entrecomilla la celda si contiene ";", comillas o saltos de línea (RFC 4180). */
    fun celdaCsv(valor: String): String =
        if (valor.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + valor.replace("\"", "\"\"") + "\""
        } else {
            valor
        }
}
