package com.cappucare.app.reglas

import com.cappucare.app.data.MockData
import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la validación de formularios (RF-00, RF-01, RF-04, RF-05 y reglas RN-02, RN-03, RN-04).
 */
class ValidacionesTest {

    private val productos = MockData.productos
    private fun producto(id: String): Producto = productos.first { it.id == id }

    // ---------- RF-00 · Login ----------

    @Test
    fun `login con clave vacia pide la clave`() {
        val errores = Validaciones.validarLogin("encargado@cappucare.cl", "")
        assertNull(errores.usuario)
        assertEquals("Ingresa tu clave", errores.clave)
    }

    @Test
    fun `login con usuario vacio pide el usuario`() {
        assertEquals("Ingresa tu usuario", Validaciones.validarLogin("  ", "x").usuario)
    }

    @Test
    fun `login con correo mal escrito muestra error de formato`() {
        assertEquals("Ingresa un correo válido", Validaciones.validarLogin("encargado", "x").usuario)
    }

    @Test
    fun `login con datos completos no tiene errores`() {
        assertFalse(Validaciones.validarLogin("encargado@cappucare.cl", "encargado123").hayErrores)
    }

    // ---------- RF-01 · Producto ----------

    private fun validarProducto(
        idActual: String? = null,
        codigo: String = "CUR-003",
        nombre: String = "Apósito adhesivo",
        categoria: CategoriaInsumo? = CategoriaInsumo.CURACION,
        unidad: UnidadMedida? = UnidadMedida.CAJA,
        nivelMinimo: String = "5",
        ubicacion: String = "Estante A3"
    ) = Validaciones.validarProducto(idActual, codigo, nombre, categoria, unidad, nivelMinimo, ubicacion, productos)

    @Test
    fun `producto nuevo con datos validos no tiene errores`() {
        assertFalse(validarProducto().hayErrores)
    }

    @Test
    fun `codigo repetido sin distinguir mayusculas muestra error (RN-04)`() {
        assertEquals("Ya existe un producto con ese código", validarProducto(codigo = "cur-001").codigo)
    }

    @Test
    fun `al editar no se compara el producto consigo mismo`() {
        assertNull(validarProducto(idActual = "P1", codigo = "CUR-001").codigo)
    }

    @Test
    fun `nivel minimo no numerico o negativo es invalido`() {
        assertEquals("Ingresa un número válido", validarProducto(nivelMinimo = "abc").nivelMinimo)
        assertEquals("Ingresa un número válido", validarProducto(nivelMinimo = "-1").nivelMinimo)
        assertNull(validarProducto(nivelMinimo = "0").nivelMinimo)
    }

    @Test
    fun `campos obligatorios y listas sin elegir`() {
        val errores = validarProducto(codigo = "", nombre = "", categoria = null, unidad = null, ubicacion = "")
        assertEquals("Campo obligatorio", errores.codigo)
        assertEquals("Campo obligatorio", errores.nombre)
        assertEquals("Elige una categoría", errores.categoria)
        assertEquals("Elige una unidad de medida", errores.unidadMedida)
        assertEquals("Campo obligatorio", errores.ubicacion)
    }

    // ---------- RF-04 / RF-05 · Movimientos ----------

    @Test
    fun `movimiento sin producto pide elegir uno`() {
        assertEquals("Elige un producto", Validaciones.validarMovimiento(null, TipoMovimiento.INGRESO, "5", null).producto)
    }

    @Test
    fun `cantidad 0 o texto es invalida`() {
        val p = producto("P2")
        assertEquals("Ingresa una cantidad mayor que 0", Validaciones.validarMovimiento(p, TipoMovimiento.INGRESO, "0", null).cantidad)
        assertEquals("Ingresa una cantidad mayor que 0", Validaciones.validarMovimiento(p, TipoMovimiento.INGRESO, "abc", null).cantidad)
    }

    @Test
    fun `salida mayor que lo disponible muestra cuantas hay (RN-03)`() {
        val errores = Validaciones.validarMovimiento(producto("P4"), TipoMovimiento.SALIDA, "5", MotivoSalida.VENTA)
        assertEquals("Solo hay 3 disponibles", errores.cantidad)
    }

    @Test
    fun `salida sin motivo es invalida (RN-02)`() {
        val errores = Validaciones.validarMovimiento(producto("P1"), TipoMovimiento.SALIDA, "5", null)
        assertEquals("Elige el motivo de la salida", errores.motivo)
    }

    @Test
    fun `no se puede vender un producto vencido`() {
        val vencido = producto("P5").copy(estado = EstadoProducto.VENCIDO)
        val errores = Validaciones.validarMovimiento(vencido, TipoMovimiento.SALIDA, "1", MotivoSalida.VENTA)
        assertTrue(errores.motivo!!.startsWith("No se puede vender"))
        // pero sí se puede sacar por vencimiento
        assertNull(Validaciones.validarMovimiento(vencido, TipoMovimiento.SALIDA, "1", MotivoSalida.VENCIMIENTO).motivo)
    }

    @Test
    fun `ingreso valido no pide motivo`() {
        assertFalse(Validaciones.validarMovimiento(producto("P2"), TipoMovimiento.INGRESO, "20", null).hayErrores)
    }

    @Test
    fun `un producto dado de baja no admite ingresos`() {
        val baja = producto("P3").copy(estado = EstadoProducto.DADO_DE_BAJA)
        val errores = Validaciones.validarMovimiento(baja, TipoMovimiento.INGRESO, "5", null)
        assertEquals("Un producto dado de baja no admite ingresos", errores.producto)
        assertEquals(errores.producto, errores.primerMensaje)
    }

    @Test
    fun `un producto dado de baja si admite una salida por ajuste`() {
        val baja = producto("P3").copy(estado = EstadoProducto.DADO_DE_BAJA)
        assertFalse(Validaciones.validarMovimiento(baja, TipoMovimiento.SALIDA, "2", MotivoSalida.AJUSTE).hayErrores)
    }

    @Test
    fun `el maximo de unidades por movimiento se respeta`() {
        val errores = Validaciones.validarMovimiento(producto("P2"), TipoMovimiento.INGRESO, "100001", null)
        assertEquals("El máximo es 100000", errores.cantidad)
    }

    @Test
    fun `sin errores no hay primer mensaje`() {
        assertNull(Validaciones.validarMovimiento(producto("P2"), TipoMovimiento.INGRESO, "20", null).primerMensaje)
    }
}
