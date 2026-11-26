package com.cappucare.app.reglas

import com.cappucare.app.data.MockData
import com.cappucare.app.model.Perfil
import com.cappucare.app.model.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integridad de los datos ficticios de ejemplo (RNF-04, RN-01, RN-04, RN-05, RN-08). */
class MockDataTest {

    @Test
    fun `para cada producto ingresos menos salidas es igual a su cantidad (RN-01)`() {
        MockData.productos.forEach { producto ->
            val movimientos = MockData.movimientos.filter { it.productoId == producto.id }
            val neto = movimientos.sumOf { if (it.tipo == TipoMovimiento.INGRESO) it.cantidad else -it.cantidad }
            assertEquals("Producto ${producto.id}", producto.cantidadDisponible, neto)
        }
    }

    @Test
    fun `los codigos de producto son unicos sin distinguir mayusculas (RN-04)`() {
        val codigos = MockData.productos.map { it.codigo.lowercase() }
        assertEquals(codigos.size, codigos.toSet().size)
    }

    @Test
    fun `todo movimiento apunta a un producto existente, tiene fecha ordenable y responsable (RN-05)`() {
        val ids = MockData.productos.map { it.id }.toSet()
        MockData.movimientos.forEach { m ->
            assertTrue(m.productoId in ids)
            assertTrue(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}").matches(m.fecha))
            assertTrue(m.responsable.isNotBlank())
        }
    }

    @Test
    fun `solo las salidas tienen motivo (RN-02)`() {
        MockData.movimientos.forEach { m ->
            assertEquals(m.tipo == TipoMovimiento.SALIDA, m.motivo != null)
        }
    }

    @Test
    fun `hay un usuario por perfil y sus claves no estan en texto plano (RNF-08)`() {
        assertEquals(setOf(Perfil.ENCARGADO, Perfil.USUARIO_AUTORIZADO), MockData.usuarios.map { it.perfil }.toSet())
        MockData.usuarios.forEach { assertEquals(64, it.claveHash.length) }
        val encargado = MockData.usuarios.first { it.perfil == Perfil.ENCARGADO }
        assertTrue(Seguridad.verificar("encargado123", encargado.claveHash))
        assertFalse(Seguridad.verificar("ventas123", encargado.claveHash))
    }

    @Test
    fun `solo el personal encargado edita el catalogo (RN-08)`() {
        assertTrue(Perfil.ENCARGADO.puedeEditarCatalogo)
        assertFalse(Perfil.USUARIO_AUTORIZADO.puedeEditarCatalogo)
    }
}
