package com.cappucare.app.data.local

import androidx.room.withTransaction
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [AlmacenLocal] sobre Room: la base del MODO LOCAL (archivo "cappucare_local.db"). */
class AlmacenRoom(private val db: AppDatabase) : AlmacenLocal {

    private val usuarioDao = db.usuarioDao()
    private val productoDao = db.productoDao()
    private val movimientoDao = db.movimientoDao()

    override val productos: Flow<List<Producto>> =
        productoDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    override val movimientos: Flow<List<Movimiento>> =
        movimientoDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    override suspend fun buscarUsuario(usuario: String): Usuario? =
        usuarioDao.buscarPorUsuario(usuario.trim())?.aModelo()

    override suspend fun productosActuales(): List<Producto> =
        productoDao.todos().map { it.aModelo() }

    override suspend fun guardarProducto(producto: Producto) {
        productoDao.guardar(producto.aEntidad())
    }

    override suspend fun guardarMovimiento(movimiento: Movimiento, producto: Producto) {
        db.withTransaction {
            movimientoDao.insertar(movimiento.aEntidad())
            productoDao.actualizar(producto.aEntidad())
        }
    }

    /** Movimientos antes que productos al borrar y productos antes que movimientos al insertar (clave foránea). */
    override suspend fun reemplazarDatos(productos: List<Producto>, movimientos: List<Movimiento>) {
        db.withTransaction {
            movimientoDao.borrarTodos()
            productoDao.borrarTodos()
            productoDao.guardarVarios(productos.map { it.aEntidad() })
            movimientoDao.guardarVarios(movimientos.map { it.aEntidad() })
        }
    }

    override suspend fun <T> enTransaccion(bloque: suspend () -> T): T = db.withTransaction { bloque() }
}
