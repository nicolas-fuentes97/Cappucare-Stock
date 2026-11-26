package com.cappucare.app.reglas

import com.cappucare.app.data.MockData
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.Producto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias del RF-09 / RN-07 (se ejecutan en el PC, sin emulador).
 * En Android Studio: clic derecho sobre esta clase > Run 'ReglaReposicionTest'.
 * Los casos salen de los "Criterios de aceptación" y "Casos de prueba" de los requerimientos del caso.
 */
class ReglaReposicionTest {

    private val productos = MockData.productos

    private fun producto(id: String): Producto = productos.first { it.id == id }

    /** Simula el efecto de un movimiento sobre la lista (lo que hará registrarMovimiento()). */
    private fun conCantidad(id: String, nuevaCantidad: Int): List<Producto> =
        productos.map { if (it.id == id) it.copy(cantidadDisponible = nuevaCantidad) else it }

    @Test
    fun `con los datos de ejemplo las alertas son P2, P4 y P6`() {
        val alertas = ReglaReposicion.filtrarBajoMinimo(productos)
        assertEquals(setOf("P2", "P4", "P6"), alertas.map { it.id }.toSet())
        assertEquals(3, alertas.size)
    }

    @Test
    fun `P2 y P4 faltan 7 y se desempata por nombre, luego P6 que falta 3`() {
        val alertas = ReglaReposicion.filtrarBajoMinimo(productos)
        assertEquals(listOf("P4", "P2", "P6"), alertas.map { it.id }) // Mascarilla antes que Venda
    }

    @Test
    fun `P5 con 6 unidades y minimo 5 no aparece`() {
        assertFalse(ReglaReposicion.estaBajoMinimo(producto("P5")))
    }

    @Test
    fun `bajo el minimo es menor estricto - igual al minimo no alerta`() {
        val p = producto("P2").copy(cantidadDisponible = 15) // 15 de 15
        assertFalse(ReglaReposicion.estaBajoMinimo(p))
        assertTrue(ReglaReposicion.estaBajoMinimo(p.copy(cantidadDisponible = 14)))
    }

    @Test
    fun `un producto dado de baja no se considera`() {
        val p = producto("P4").copy(estado = EstadoProducto.DADO_DE_BAJA)
        assertFalse(ReglaReposicion.estaBajoMinimo(p))
    }

    @Test
    fun `un producto agotado si aparece en las alertas`() {
        assertTrue(ReglaReposicion.estaBajoMinimo(producto("P6"))) // 0 de 3, estado AGOTADO
    }

    @Test
    fun `al registrar un ingreso de 10 en P4 sale de la lista y el contador baja en 1`() {
        val antes = ReglaReposicion.filtrarBajoMinimo(productos)
        val despues = ReglaReposicion.filtrarBajoMinimo(conCantidad("P4", 3 + 10))
        assertFalse(despues.any { it.id == "P4" })
        assertEquals(antes.size - 1, despues.size)
    }

    @Test
    fun `ordena por mayor falta`() {
        val faltas = ReglaReposicion.filtrarBajoMinimo(productos).map { ReglaReposicion.faltaParaMinimo(it) }
        assertEquals(faltas.sortedDescending(), faltas)
    }

    @Test
    fun `la falta nunca es negativa`() {
        assertEquals(0, ReglaReposicion.faltaParaMinimo(producto("P1"))) // 40 de 20
        assertEquals(7, ReglaReposicion.faltaParaMinimo(producto("P4"))) // 3 de 10
    }

    @Test
    fun `aviso cuando un movimiento hace cruzar el minimo`() {
        // P1: 40 -> 15 con minimo 20 -> cruza el minimo: un aviso.
        assertTrue(ReglaReposicion.cruzoElMinimo(producto("P1"), producto("P1").copy(cantidadDisponible = 15)))
    }

    @Test
    fun `no se repite el aviso si el producto ya estaba bajo el minimo`() {
        // P2 ya estaba en 8 de 15; otra venta lo deja en 5: no hay aviso nuevo.
        assertFalse(ReglaReposicion.cruzoElMinimo(producto("P2"), producto("P2").copy(cantidadDisponible = 5)))
    }

    @Test
    fun `no hay aviso si el producto sigue al dia`() {
        assertFalse(ReglaReposicion.cruzoElMinimo(producto("P1"), producto("P1").copy(cantidadDisponible = 30)))
    }

    @Test
    fun `un ingreso no genera aviso`() {
        assertFalse(ReglaReposicion.cruzoElMinimo(producto("P2"), producto("P2").copy(cantidadDisponible = 28)))
    }
}
