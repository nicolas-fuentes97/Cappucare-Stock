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
 * Hay DOS archivos con las mismas tablas y la misma carga inicial: "cappucare.db" (modo servidor:
 * copia de la API) y "cappucare_local.db" (modo local: datos propios del equipo, independientes).
 * Instancia única por archivo (patrón Singleton).
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
        /** Base del modo SERVIDOR (copia local de lo que entrega la API). */
        const val NOMBRE_SERVIDOR = "cappucare.db"

        /** Base del modo LOCAL: independiente de la anterior (no se sincroniza con el servidor). */
        const val NOMBRE_LOCAL = "cappucare_local.db"

        private val instancias = HashMap<String, AppDatabase>()

        /** Una instancia por archivo (patrón Singleton): cada modo comparte su propia conexión. */
        fun obtener(context: Context, nombre: String = NOMBRE_SERVIDOR): AppDatabase =
            synchronized(this) {
                instancias.getOrPut(nombre) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        nombre
                    )
                        .addCallback(CargaInicial)
                        .build()
                }
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
