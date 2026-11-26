package com.cappucare.app.reglas

import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.ErroresLogin
import com.cappucare.app.model.ErroresMovimiento
import com.cappucare.app.model.ErroresProducto
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida

/**
 * Validación de todos los formularios, centralizada y desacoplada de la interfaz:
 * devuelve un objeto de errores con un mensaje por campo (null = sin error) y la pantalla solo
 * lo muestra bajo cada campo. Kotlin puro: se prueba en ValidacionesTest.
 */
object Validaciones {

    const val CANTIDAD_MAXIMA = 100_000

    /** RN-04: mensaje del código repetido (la validación y el modo local lo comparten). */
    const val MENSAJE_CODIGO_REPETIDO = "Ya existe un producto con ese código"

    private val formatoCorreo = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val formatoCodigo = Regex("^[A-Za-z0-9-]{3,20}$")

    /** RF-00 · Login: campos obligatorios y formato de correo. */
    fun validarLogin(usuario: String, clave: String): ErroresLogin = ErroresLogin(
        usuario = when {
            usuario.isBlank() -> "Ingresa tu usuario"
            !formatoCorreo.matches(usuario.trim()) -> "Ingresa un correo válido"
            else -> null
        },
        clave = if (clave.isBlank()) "Ingresa tu clave" else null
    )

    /**
     * RF-01 · Formulario de producto.
     * @param idActual id del producto que se edita (null o "" = producto nuevo), para no
     *                 compararlo consigo mismo en RN-04.
     */
    fun validarProducto(
        idActual: String?,
        codigo: String,
        nombre: String,
        categoria: CategoriaInsumo?,
        unidadMedida: UnidadMedida?,
        nivelMinimo: String,
        ubicacion: String,
        existentes: List<Producto>
    ): ErroresProducto {
        val codigoLimpio = codigo.trim()
        val nivel = nivelMinimo.trim().toIntOrNull()
        return ErroresProducto(
            codigo = when {
                codigoLimpio.isEmpty() -> "Campo obligatorio"
                !formatoCodigo.matches(codigoLimpio) -> "Usa de 3 a 20 letras, números o guiones (ej: CUR-003)"
                existentes.any { it.id != idActual && it.codigo.trim().equals(codigoLimpio, ignoreCase = true) } ->
                    MENSAJE_CODIGO_REPETIDO   // RN-04
                else -> null
            },
            nombre = when {
                nombre.isBlank() -> "Campo obligatorio"
                nombre.trim().length < 3 -> "Mínimo 3 caracteres"
                else -> null
            },
            categoria = if (categoria == null) "Elige una categoría" else null,
            unidadMedida = if (unidadMedida == null) "Elige una unidad de medida" else null,
            nivelMinimo = when {
                nivel == null || nivel < 0 -> "Ingresa un número válido"
                nivel > CANTIDAD_MAXIMA -> "El máximo es $CANTIDAD_MAXIMA"
                else -> null
            },
            ubicacion = if (ubicacion.isBlank()) "Campo obligatorio" else null
        )
    }

    /**
     * RF-04 / RF-05 · Formulario de movimiento.
     *  - producto obligatorio; cantidad entera > 0;
     *  - RN-03: una salida no puede superar la cantidad disponible;
     *  - RN-02: toda salida tiene motivo;
     *  - decisión de diseño: no se vende un producto VENCIDO o DADO_DE_BAJA, y un producto dado
     *    de baja no recibe ingresos (se mantiene fuera de circulación).
     */
    fun validarMovimiento(
        producto: Producto?,
        tipo: TipoMovimiento,
        cantidad: String,
        motivo: MotivoSalida?
    ): ErroresMovimiento {
        val n = cantidad.trim().toIntOrNull()
        val esSalida = tipo == TipoMovimiento.SALIDA
        return ErroresMovimiento(
            producto = when {
                producto == null -> "Elige un producto"
                tipo == TipoMovimiento.INGRESO && producto.estado == EstadoProducto.DADO_DE_BAJA ->
                    "Un producto dado de baja no admite ingresos"
                else -> null
            },
            cantidad = when {
                n == null || n <= 0 -> "Ingresa una cantidad mayor que 0"
                n > CANTIDAD_MAXIMA -> "El máximo es $CANTIDAD_MAXIMA"
                esSalida && producto != null && n > producto.cantidadDisponible ->
                    "Solo hay ${producto.cantidadDisponible} disponibles"
                else -> null
            },
            motivo = when {
                esSalida && motivo == null -> "Elige el motivo de la salida"
                esSalida && motivo == MotivoSalida.VENTA && producto != null &&
                    (producto.estado == EstadoProducto.VENCIDO || producto.estado == EstadoProducto.DADO_DE_BAJA) ->
                    "No se puede vender un producto en estado ${producto.estado.etiqueta.lowercase()}"
                else -> null
            }
        )
    }
}
