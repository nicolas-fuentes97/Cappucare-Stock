package com.cappucare.app.reglas

import com.cappucare.app.data.MockData
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Perfil
import com.cappucare.app.model.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de RF-10 (historial) y RN-06, con los datos de ejemplo. */
class ReglasHistorialTest {

    private val movimientos = MockData.movimientos
    private val productos = MockData.productos

    @Test
    fun `historial de P1 muestra M2 y luego M1`() {
        val historial = ReglasHistorial.historial(movimientos, "P1")
        assertEquals(listOf("M2", "M1"), historial.map { it.id })
    }

    @Test
    fun `historial completo muestra los 11 movimientos, el mas reciente primero`() {
        val historial = ReglasHistorial.historial(movimientos, null)
        assertEquals(11, historial.size)
        assertEquals(listOf("M9", "M6", "M2", "M11"), historial.take(4).map { it.id })
        assertEquals("M1", historial.last().id)
        assertEquals(historial.map { it.fecha }.sortedDescending(), historial.map { it.fecha })
    }

    @Test
    fun `un producto sin movimientos da una lista vacia`() {
        assertTrue(ReglasHistorial.historial(movimientos, "NO-EXISTE").isEmpty())
    }

    @Test
    fun `con el mismo minuto se respeta el orden de entrada`() {
        val a = movimientos.first { it.id == "M1" }.copy(id = "A", fecha = "2026-10-05 10:00")
        val b = a.copy(id = "B")
        assertEquals(listOf("B", "A"), ReglasHistorial.historial(listOf(b, a), null).map { it.id })
    }

    @Test
    fun `el saldo del movimiento mas reciente es la cantidad actual del producto`() {
        val p1 = ReglasHistorial.conSaldo(movimientos, productos, "P1")
        assertEquals(listOf("M2", "M1"), p1.map { it.movimiento.id })
        assertEquals(40, p1[0].saldo)   // despues de la venta: 50 - 10 = 40
        assertEquals(50, p1[1].saldo)   // despues del ingreso inicial
    }

    @Test
    fun `el saldo parte de cero con el primer ingreso y llega a la cantidad actual`() {
        productos.forEach { producto ->
            val lista = ReglasHistorial.conSaldo(movimientos, productos, producto.id)
            assertEquals(producto.cantidadDisponible, lista.first().saldo)
            val primero = lista.last()
            assertEquals(primero.movimiento.cantidad, primero.saldo) // el primero fue un ingreso desde 0
        }
    }

    @Test
    fun `filtrar por tipo no cambia los saldos`() {
        val salidas = ReglasHistorial.conSaldo(movimientos, productos, null, TipoMovimiento.SALIDA)
        assertTrue(salidas.all { it.movimiento.tipo == TipoMovimiento.SALIDA })
        assertEquals(listOf("M9", "M6", "M2"), salidas.map { it.movimiento.id })
        assertEquals(3, salidas.first { it.movimiento.id == "M6" }.saldo) // P4: 10 - 7
    }

    @Test
    fun `el resumen suma ingresos y salidas`() {
        assertEquals(ResumenHistorial(totalIngresos = 50, totalSalidas = 10), ReglasHistorial.resumen(movimientos, "P1"))
        assertEquals(ResumenHistorial(totalIngresos = 116, totalSalidas = 18), ReglasHistorial.resumen(movimientos, null))
    }

    @Test
    fun `cada movimiento guarda quien lo registro y su perfil (RF-06)`() {
        val m2 = movimientos.first { it.id == "M2" }
        assertEquals("Vendedor autorizado (demo)", m2.responsable)
        assertEquals(Perfil.USUARIO_AUTORIZADO, m2.responsablePerfil)
        assertEquals(Perfil.ENCARGADO, movimientos.first { it.id == "M1" }.responsablePerfil)
    }

    // ---------- CSV ----------

    @Test
    fun `el CSV trae encabezado y una fila por movimiento`() {
        val items = ReglasHistorial.conSaldo(movimientos, productos, "P1")
        val lineas = ReglasHistorial.aCsv(items, productos).trimEnd().split("\r\n")
        assertEquals(3, lineas.size)
        assertEquals("Fecha;Código;Producto;Tipo;Cantidad;Motivo;Saldo;Responsable;Perfil;Observación", lineas[0])
        assertEquals(
            "2026-10-02 16:40;CUR-001;Gasa estéril 10x10 cm;Salida;10;Venta;40;Vendedor autorizado (demo);Usuario autorizado;",
            lineas[1]
        )
    }

    @Test
    fun `las celdas con punto y coma, comillas o saltos de linea se entrecomillan`() {
        assertEquals("simple", ReglasHistorial.celdaCsv("simple"))
        assertEquals("\"a;b\"", ReglasHistorial.celdaCsv("a;b"))
        assertEquals("\"dijo \"\"hola\"\"\"", ReglasHistorial.celdaCsv("dijo \"hola\""))
        assertEquals("\"linea1\nlinea2\"", ReglasHistorial.celdaCsv("linea1\nlinea2"))
    }

    @Test
    fun `un movimiento con observacion especial no rompe las columnas`() {
        val raro = Movimiento(
            "X", "P1", "2026-10-06 08:00", TipoMovimiento.SALIDA, 1, MotivoSalida.OTRO,
            "caja; dañada", "Encargada de bodega (demo)", Perfil.ENCARGADO
        )
        val csv = ReglasHistorial.aCsv(listOf(MovimientoConSaldo(raro, 39)), productos)
        assertTrue(csv.contains(";\"caja; dañada\"\r\n"))
    }
}
