package com.cappucare.app.data.local

import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Almacenamiento del MODO LOCAL: lo mínimo que [com.cappucare.app.data.repository.FuenteLocal]
 * necesita de la base. La implementación real usa Room ([AlmacenRoom]) y las pruebas JVM usan una
 * copia en memoria, sin Android. Las reglas de negocio NO están aquí: este contrato solo guarda y lee.
 */
interface AlmacenLocal {

    val productos: Flow<List<Producto>>

    /** Del más reciente al más antiguo. */
    val movimientos: Flow<List<Movimiento>>

    suspend fun buscarUsuario(usuario: String): Usuario?

    suspend fun productosActuales(): List<Producto>

    /** Inserta o reemplaza un producto (por id). */
    suspend fun guardarProducto(producto: Producto)

    /** Guarda el movimiento y el producto con su cantidad nueva (RNF-03: ambas cosas o ninguna). */
    suspend fun guardarMovimiento(movimiento: Movimiento, producto: Producto)

    /**
     * Reemplaza TODOS los productos y movimientos (los usuarios no cambian). Los movimientos se
     * entregan en orden de registro: el más antiguo primero.
     */
    suspend fun reemplazarDatos(productos: List<Producto>, movimientos: List<Movimiento>)

    /** Ejecuta [bloque] como una sola unidad (en Room, una transacción). */
    suspend fun <T> enTransaccion(bloque: suspend () -> T): T
}
