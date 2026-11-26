package com.cappucare.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/*
 * DAOs (Data Access Objects): las consultas a la base de datos.
 *  - Funciones "suspend": se ejecutan una vez, fuera del hilo principal (RNF-09).
 *  - Funciones que devuelven Flow: Room vuelve a emitir la lista cada vez que la tabla
 *    cambia, así las pantallas se actualizan solas.
 */

@Dao
interface UsuarioDao {

    /** RF-00: busca por usuario sin distinguir mayúsculas. La clave se verifica en FuenteLocal. */
    @Query("SELECT * FROM usuarios WHERE lower(usuario) = lower(:usuario) LIMIT 1")
    suspend fun buscarPorUsuario(usuario: String): UsuarioEntity?
}

@Dao
interface ProductoDao {

    /**
     * RF-02 / RF-08: catálogo completo ordenado por nombre (Flow: se actualiza solo). La búsqueda,
     * el filtro por categoría y las alertas de reposición se calculan sobre esta lista con las
     * reglas de reglas/ (una sola implementación de cada regla, probada con JUnit).
     */
    @Query("SELECT * FROM productos ORDER BY nombre COLLATE NOCASE")
    fun observarTodos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos ORDER BY nombre COLLATE NOCASE")
    suspend fun todos(): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun buscarPorId(id: String): ProductoEntity?

    /** RF-01 (nuevo). Si el código ya existe, el índice único lanza una excepción (RN-04). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(producto: ProductoEntity)

    /** RF-01 (editar) y RF-07 (nueva cantidad tras un movimiento). */
    @Update
    suspend fun actualizar(producto: ProductoEntity)

    /** RF-03: cambia solo la columna estado. */
    @Query("UPDATE productos SET estado = :estado WHERE id = :id")
    suspend fun actualizarEstado(id: String, estado: String)

    /** Inserta o actualiza un producto (crear y editar desde FuenteLocal). */
    @Upsert
    suspend fun guardar(producto: ProductoEntity)

    /** Inserta o actualiza varios productos ("Restablecer datos de ejemplo"). */
    @Upsert
    suspend fun guardarVarios(productos: List<ProductoEntity>)

    /**
     * Eliminar un producto (DELETE del CRUD). Solo se llama cuando el producto NO tiene movimientos:
     * FuenteLocal lo revisa antes y, además, la clave foránea RESTRICT de "movimientos" impide
     * borrar un producto con historial (RN-06).
     */
    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun eliminarPorId(id: String): Int

    /** Solo para "Restablecer datos de ejemplo", dentro de una transacción. */
    @Query("DELETE FROM productos")
    suspend fun borrarTodos()
}

/**
 * RN-06: los movimientos solo se insertan y se consultan: el historial no se edita ni se elimina.
 * (borrarTodos existe únicamente para "Restablecer datos de ejemplo".)
 */
@Dao
interface MovimientoDao {

    /** RF-10: del más reciente al más antiguo (rowid desempata los del mismo minuto). */
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, rowid DESC")
    fun observarTodos(): Flow<List<MovimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(movimiento: MovimientoEntity)

    /** Cuántos movimientos tiene un producto (para decidir si se puede eliminar). */
    @Query("SELECT COUNT(*) FROM movimientos WHERE productoId = :productoId")
    suspend fun contarDeProducto(productoId: String): Int

    @Upsert
    suspend fun guardarVarios(movimientos: List<MovimientoEntity>)

    /** Solo para "Restablecer datos de ejemplo": vuelve a los datos de ejemplo. */
    @Query("DELETE FROM movimientos")
    suspend fun borrarTodos()
}
