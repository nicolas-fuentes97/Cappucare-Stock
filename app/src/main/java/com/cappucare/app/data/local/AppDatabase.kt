package com.cappucare.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cappucare.app.data.MockData

/**
 * Base de datos local (Room sobre SQLite) — RT-02 / RNF-01: los datos sobreviven al cierre
 * de la app y funcionan sin conexión.
 *
 * Un solo archivo, "cappucare_local.db", con una instancia única para toda la app (patrón Singleton).
 * Si se cambia una entidad durante el desarrollo: subir "version" (y agregar la migración) o
 * desinstalar la app.
 */
@Database(
    entities = [UsuarioEntity::class, ProductoEntity::class, MovimientoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao

    companion object {
        /** Archivo de la base (el mismo nombre que usaba el modo local de la plantilla). */
        const val NOMBRE = "cappucare_local.db"

        @Volatile
        private var instancia: AppDatabase? = null

        /** Instancia única (patrón Singleton): toda la app comparte la misma conexión. */
        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE
                )
                    .addCallback(CargaInicial)
                    .build()
                    .also { instancia = it }
            }
    }
}


/**
 * Carga inicial: la PRIMERA vez que se crea la base inserta los datos ficticios de MockData
 * (2 usuarios de prueba, 8 productos y 11 movimientos cuadrados con el stock).
 *
 * Se hace con inserciones directas dentro de onCreate (que Room ejecuta en el mismo paso que la
 * creación de las tablas): así los datos existen antes de la primera consulta y no hay carreras
 * con el inicio de sesión. Productos antes que movimientos por la clave foránea.
 */
private object CargaInicial : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        MockData.usuarios.map { it.aEntidad() }.forEach { u ->
            db.insert(
                "usuarios", SQLiteDatabase.CONFLICT_IGNORE,
                contentValuesOf(
                    "id" to u.id, "nombre" to u.nombre, "usuario" to u.usuario,
                    "claveHash" to u.claveHash, "perfil" to u.perfil
                )
            )
        }
        MockData.productos.map { it.aEntidad() }.forEach { p ->
            db.insert(
                "productos", SQLiteDatabase.CONFLICT_IGNORE,
                contentValuesOf(
                    "id" to p.id, "codigo" to p.codigo, "nombre" to p.nombre,
                    "descripcion" to p.descripcion, "categoria" to p.categoria,
                    "unidadMedida" to p.unidadMedida, "cantidadDisponible" to p.cantidadDisponible,
                    "nivelMinimo" to p.nivelMinimo, "ubicacion" to p.ubicacion, "estado" to p.estado
                )
            )
        }
        MockData.movimientos.map { it.aEntidad() }.forEach { m ->
            db.insert(
                "movimientos", SQLiteDatabase.CONFLICT_IGNORE,
                contentValuesOf(
                    "id" to m.id, "productoId" to m.productoId, "fecha" to m.fecha,
                    "tipo" to m.tipo, "cantidad" to m.cantidad, "motivo" to m.motivo,
                    "observacion" to m.observacion, "responsable" to m.responsable,
                    "responsablePerfil" to m.responsablePerfil
                )
            )
        }
    }
}
