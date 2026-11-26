package com.cappucare.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * ENTIDADES de Room = tablas de la base de datos SQLite (modelo relacional del proyecto).
 *
 *   usuarios (1)            productos (1) ──< (N) movimientos
 *
 * Los enums (perfil, categoría, unidad, estado, tipo, motivo) se guardan como texto con
 * enum.name y se convierten de vuelta en Mapeos.kt.
 */

/** Tabla "usuarios": personal que inicia sesión (RF-00). Solo datos ficticios (RNF-04). */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["usuario"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val usuario: String,
    val claveHash: String,      // SHA-256 con sal (reglas/Seguridad.kt), nunca la clave en texto plano
    val perfil: String
)

/**
 * Tabla "productos" (RF-01). RN-04: el código es único → índice único en "codigo".
 * RN-01: cantidadDisponible solo cambia dentro de la @Transaction que registra un movimiento.
 */
@Entity(
    tableName = "productos",
    indices = [Index(value = ["codigo"], unique = true)]
)
data class ProductoEntity(
    @PrimaryKey val id: String,
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val unidadMedida: String,
    val cantidadDisponible: Int,
    val nivelMinimo: Int,
    val ubicacion: String,
    val estado: String
)

/**
 * Tabla "movimientos" (RF-04, RF-05, RF-06). Relación N:1 con productos mediante la clave
 * foránea productoId. RN-06: los movimientos no se editan ni se eliminan (el DAO no tiene
 * @Update ni @Delete).
 */
@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["productoId"])]
)
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val productoId: String,
    val fecha: String,              // "yyyy-MM-dd HH:mm": se puede ordenar como texto
    val tipo: String,               // INGRESO o SALIDA
    val cantidad: Int,
    val motivo: String?,            // solo en salidas (RN-02)
    val observacion: String,
    val responsable: String,        // nombre del usuario con sesión iniciada (RF-06)
    val responsablePerfil: String   // perfil de ese usuario (RF-06)
)
