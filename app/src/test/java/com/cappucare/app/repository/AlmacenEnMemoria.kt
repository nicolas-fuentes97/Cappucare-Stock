package com.cappucare.app.repository

import com.cappucare.app.data.MockData
import com.cappucare.app.data.local.AlmacenLocal
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Base local en memoria: reemplaza a Room en las pruebas JVM del modo local (sin Android). */
class AlmacenEnMemoria(private val usuarios: List<Usuario> = MockData.usuarios) : AlmacenLocal {

    private val _productos = MutableStateFlow(MockData.productos)
    private val _movimientos = MutableStateFlow(MockData.movimientos.asReversed())   // más reciente primero

    /** Si es true, la próxima escritura de un producto falla (simula el índice único de Room). */
    var fallarAlGuardarProducto = false

    override val productos: Flow<List<Producto>> = _productos
    override val movimientos: Flow<List<Movimiento>> = _movimientos

    val productosGuardados: List<Producto> get() = _productos.value
    val movimientosGuardados: List<Movimiento> get() = _movimientos.value

    override suspend fun buscarUsuario(usuario: String): Usuario? =
        usuarios.firstOrNull { it.usuario.equals(usuario.trim(), ignoreCase = true) }

    override suspend fun productosActuales(): List<Producto> = _productos.value

    override suspend fun guardarProducto(producto: Producto) {
        if (fallarAlGuardarProducto) throw IllegalStateException("UNIQUE constraint failed: productos.codigo")
        val lista = _productos.value
        _productos.value =
            if (lista.any { it.id == producto.id }) lista.map { if (it.id == producto.id) producto else it } else lista + producto
    }

    override suspend fun guardarMovimiento(movimiento: Movimiento, producto: Producto) {
        guardarProducto(producto)
        _movimientos.value = listOf(movimiento) + _movimientos.value
    }

    override suspend fun reemplazarDatos(productos: List<Producto>, movimientos: List<Movimiento>) {
        _productos.value = productos
        _movimientos.value = movimientos.asReversed()
    }

    override suspend fun <T> enTransaccion(bloque: suspend () -> T): T = bloque()
}
