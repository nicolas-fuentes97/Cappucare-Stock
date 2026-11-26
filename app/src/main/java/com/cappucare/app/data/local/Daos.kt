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

    /** RF-00: busca por usuario sin distinguir mayúsculas. La clave se verifica en el ViewModel. */
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

    // --- EA3: Room como copia local (caché) de lo que dice el servidor ---------------------------

    /** Inserta o actualiza un producto tal como lo entregó el servidor. */
    @Upsert
    suspend fun guardar(producto: ProductoEntity)

    /** Inserta o actualiza varios productos (sin borrar y reinsertar: los movimientos los referencian). */
    @Upsert
    suspend fun guardarVarios(productos: List<ProductoEntity>)

    /** Quita de la copia local los productos que el servidor ya no tiene. */
    @Query("DELETE FROM productos WHERE id NOT IN (:idsVigentes)")
    suspend fun borrarQueNoEstan(idsVigentes: List<String>)

    /** Solo para "Restablecer datos locales" (modo local), dentro de una transacción. */
    @Query("DELETE FROM productos")
    suspend fun borrarTodos()
}

/**
 * RN-06: en modo local solo hay @Insert y consultas: el historial no se altera. Las funciones de
 * "EA3" existen únicamente para renovar la copia local con lo que responde el servidor; ninguna
 * pantalla ni el usuario puede editar o eliminar un movimiento.
 */
@Dao
interface MovimientoDao {

    /** RF-10: del más reciente al más antiguo (rowid desempata los del mismo minuto). */
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, rowid DESC")
    fun observarTodos(): Flow<List<MovimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(movimiento: MovimientoEntity)

    // --- EA3: Room como copia local (caché) de lo que dice el servidor ---------------------------

    @Upsert
    suspend fun guardar(movimiento: MovimientoEntity)

    @Upsert
    suspend fun guardarVarios(movimientos: List<MovimientoEntity>)

    /** Quita de la copia local los movimientos que el servidor no tiene (se ejecuta antes de borrar productos). */
    @Query("DELETE FROM movimientos WHERE id NOT IN (:idsVigentes)")
    suspend fun borrarQueNoEstan(idsVigentes: List<String>)

    /** Solo para "Restablecer datos locales" (modo local): vuelve a los datos de ejemplo. */
    @Query("DELETE FROM movimientos")
    suspend fun borrarTodos()
}
