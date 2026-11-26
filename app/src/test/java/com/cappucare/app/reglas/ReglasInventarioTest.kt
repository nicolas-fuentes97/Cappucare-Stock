package com.cappucare.app.reglas

import com.cappucare.app.data.MockData
import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

/**
 * Pruebas de RF-07 (cantidad automática), RF-03 (estado), RF-02/RF-08 (búsqueda y filtro) con
 * los criterios de aceptación del documento de requerimientos.
 */
class ReglasInventarioTest {

    private val productos = MockData.productos
    private fun producto(id: String): Producto = productos.first { it.id == id }

    // ---------- RF-07 / RN-01 / RN-03 ----------

    @Test
    fun `P7 con 25, ingreso de 5 y salida de 10 queda con 20`() {
        val trasIngreso = ReglasInventario.aplicarMovimiento(producto("P7"), TipoMovimiento.INGRESO, 5)
        val trasSalida = ReglasInventario.aplicarMovimiento(trasIngreso, TipoMovimiento.SALIDA, 10)
        assertEquals(20, trasSalida.cantidadDisponible)
    }

    @Test
    fun `P2 con 8 y un ingreso de 20 queda con 28`() {
        assertEquals(28, ReglasInventario.aplicarMovimiento(producto("P2"), TipoMovimiento.INGRESO, 20).cantidadDisponible)
    }

    @Test
    fun `P1 con 40 y una salida de 5 queda con 35`() {
        assertEquals(35, ReglasInventario.aplicarMovimiento(producto("P1"), TipoMovimiento.SALIDA, 5).cantidadDisponible)
    }

    @Test
    fun `P8 con 4 y una salida de 4 queda en 0 y Agotado`() {
        val resultado = ReglasInventario.aplicarMovimiento(producto("P8"), TipoMovimiento.SALIDA, 4)
        assertEquals(0, resultado.cantidadDisponible)
        assertEquals(EstadoProducto.AGOTADO, resultado.estado)
    }

    @Test
    fun `un producto agotado que recibe un ingreso vuelve a Disponible`() {
        val resultado = ReglasInventario.aplicarMovimiento(producto("P6"), TipoMovimiento.INGRESO, 3)
        assertEquals(EstadoProducto.DISPONIBLE, resultado.estado)
    }

    @Test
    fun `un producto vencido no cambia de estado solo al llegar a 0`() {
        val vencido = producto("P8").copy(estado = EstadoProducto.VENCIDO)
        assertEquals(EstadoProducto.VENCIDO, ReglasInventario.aplicarMovimiento(vencido, TipoMovimiento.SALIDA, 4).estado)
    }

    @Test
    fun `el stock nunca queda negativo (RN-03)`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ReglasInventario.aplicarMovimiento(producto("P4"), TipoMovimiento.SALIDA, 5)
        }
        assertEquals("Solo hay 3 disponibles", error.message)
    }

    @Test
    fun `una cantidad 0 o negativa se rechaza`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReglasInventario.aplicarMovimiento(producto("P1"), TipoMovimiento.INGRESO, 0)
        }
    }

    @Test
    fun `la cantidad siempre es el saldo inicial mas ingresos menos salidas (RNF-03)`() {
        // Secuencia de movimientos sobre P3 (12 unidades): la cantidad nunca se edita a mano.
        val secuencia = listOf(
            TipoMovimiento.INGRESO to 8, TipoMovimiento.SALIDA to 5, TipoMovimiento.SALIDA to 3,
            TipoMovimiento.INGRESO to 1, TipoMovimiento.SALIDA to 13
        )
        var actual = producto("P3")
        var esperado = actual.cantidadDisponible
        secuencia.forEach { (tipo, cantidad) ->
            actual = ReglasInventario.aplicarMovimiento(actual, tipo, cantidad)
            esperado += if (tipo == TipoMovimiento.INGRESO) cantidad else -cantidad
            assertEquals(esperado, actual.cantidadDisponible)
        }
        assertEquals(0, actual.cantidadDisponible)
        assertEquals(EstadoProducto.AGOTADO, actual.estado)
    }

    // ---------- RF-03 · cambio de estado ----------

    @Test
    fun `P5 Disponible puede pasar a Vencido`() {
        assertNull(ReglasInventario.validarCambioEstado(producto("P5"), EstadoProducto.VENCIDO))
    }

    @Test
    fun `elegir el mismo estado no cambia nada`() {
        val error = ReglasInventario.validarCambioEstado(producto("P5").copy(estado = EstadoProducto.VENCIDO), EstadoProducto.VENCIDO)
        assertEquals("Sin cambios: el producto ya está en estado Vencido", error)
    }

    @Test
    fun `no se puede marcar Agotado un producto con unidades`() {
        assertNotNull(ReglasInventario.validarCambioEstado(producto("P5"), EstadoProducto.AGOTADO))
    }

    @Test
    fun `no se puede marcar Disponible un producto sin unidades`() {
        val sinStock = producto("P6").copy(estado = EstadoProducto.VENCIDO)
        assertNotNull(ReglasInventario.validarCambioEstado(sinStock, EstadoProducto.DISPONIBLE))
    }

    @Test
    fun `dar de baja un producto agotado esta permitido`() {
        assertNull(ReglasInventario.validarCambioEstado(producto("P6"), EstadoProducto.DADO_DE_BAJA))
    }

    // ---------- RF-02 / RF-08 ----------

    @Test
    fun `buscar guant muestra solo los guantes`() {
        val resultado = ReglasInventario.filtrarProductos(productos, "guant", null)
        assertEquals(listOf("Guantes de nitrilo talla M"), resultado.map { it.nombre })
    }

    @Test
    fun `buscar MED- muestra el termometro y el oximetro`() {
        assertEquals(setOf("P5", "P6"), ReglasInventario.filtrarProductos(productos, "MED-", null).map { it.id }.toSet())
    }

    @Test
    fun `buscar gasa encuentra P1 con 40 disponibles`() {
        val resultado = ReglasInventario.filtrarProductos(productos, "gasa", null)
        assertEquals(listOf("P1"), resultado.map { it.id })
        assertEquals(40, resultado.single().cantidadDisponible)
    }

    @Test
    fun `filtrar Proteccion personal muestra P3 y P4`() {
        val resultado = ReglasInventario.filtrarProductos(productos, "", CategoriaInsumo.PROTECCION)
        assertEquals(setOf("P3", "P4"), resultado.map { it.id }.toSet())
    }

    @Test
    fun `texto y categoria se combinan`() {
        assertEquals(emptyList<Producto>(), ReglasInventario.filtrarProductos(productos, "gasa", CategoriaInsumo.PROTECCION))
        assertEquals(listOf("P4"), ReglasInventario.filtrarProductos(productos, "mascar", CategoriaInsumo.PROTECCION).map { it.id })
    }

    @Test
    fun `sin texto ni categoria se ven los 8 productos`() {
        assertEquals(8, ReglasInventario.filtrarProductos(productos, "  ", null).size)
    }

    @Test
    fun `la busqueda ignora mayusculas y espacios al inicio y al final`() {
        assertEquals(listOf("P1"), ReglasInventario.filtrarProductos(productos, "  CUR-001 ", null).map { it.id })
    }

    @Test
    fun `buscar por codigo escaneado ignora mayusculas y espacios`() {
        assertEquals("P1", ReglasInventario.buscarPorCodigo(productos, " cur-001 ")?.id)
        assertNull(ReglasInventario.buscarPorCodigo(productos, "XYZ-999"))
    }

    @Test
    fun `con 200 productos la busqueda sigue respondiendo (RNF-09)`() {
        val catalogo = (1..200).map { n ->
            Producto(
                id = "X$n", codigo = "TST-%03d".format(n), nombre = "Insumo de prueba $n", descripcion = "",
                categoria = CategoriaInsumo.entries[n % CategoriaInsumo.entries.size],
                unidadMedida = UnidadMedida.UNIDAD, cantidadDisponible = n, nivelMinimo = 10, ubicacion = "Bodega"
            )
        }
        val inicio = System.nanoTime()
        val resultado = ReglasInventario.filtrarProductos(catalogo, "tst-15", null)
        val milisegundos = (System.nanoTime() - inicio) / 1_000_000
        assertEquals((150..159).map { "X$it" }.toSet(), resultado.map { it.id }.toSet())
        assertTrue("La busqueda tardo $milisegundos ms", milisegundos < 500)
    }

    // ---------- RN-05 ----------

    @Test
    fun `la fecha tiene formato ordenable AAAA-MM-DD HH-mm`() {
        assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}", ReglasInventario.fechaActual()))
    }
}
