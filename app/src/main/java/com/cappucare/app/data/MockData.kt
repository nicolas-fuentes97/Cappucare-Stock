package com.cappucare.app.data

import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Perfil
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import com.cappucare.app.model.Usuario
import com.cappucare.app.reglas.Seguridad

/**
 * Datos FICTICIOS de ejemplo (el caso exige no usar datos reales de la organización, de
 * clientes ni de trabajadores: RNF-04). Se cargan en Room la PRIMERA vez que se crea la base
 * de datos (ver data/local/AppDatabase.kt) y también los usan las pruebas unitarias.
 *
 * Los movimientos de ejemplo están cuadrados: para cada producto, ingresos menos salidas es
 * igual a su cantidad disponible (RN-01). Desde ahí la cantidad solo cambia con movimientos.
 */
object MockData {

    private const val ENCARGADA = "Encargada de bodega (demo)"
    private const val VENDEDOR = "Vendedor autorizado (demo)"

    /** Usuarios de prueba para el login (RF-00). Las claves se guardan como resumen SHA-256. */
    val usuarios = listOf(
        Usuario("U1", ENCARGADA, "encargado@cappucare.cl", Seguridad.hash("encargado123"), Perfil.ENCARGADO),
        Usuario("U2", VENDEDOR, "ventas@cappucare.cl", Seguridad.hash("ventas123"), Perfil.USUARIO_AUTORIZADO),
    )

    val productos = listOf(
        Producto("P1", "CUR-001", "Gasa estéril 10x10 cm", "Sobre de gasa estéril para curaciones", CategoriaInsumo.CURACION, UnidadMedida.PAQUETE, 40, 20, "Estante A1"),
        Producto("P2", "CUR-002", "Venda elástica 10 cm", "Venda elástica de uso general", CategoriaInsumo.CURACION, UnidadMedida.UNIDAD, 8, 15, "Estante A2"),
        Producto("P3", "PRO-001", "Guantes de nitrilo talla M", "Caja de 100 guantes desechables", CategoriaInsumo.PROTECCION, UnidadMedida.CAJA, 12, 10, "Estante B1"),
        Producto("P4", "PRO-002", "Mascarilla quirúrgica", "Caja de 50 mascarillas de tres pliegues", CategoriaInsumo.PROTECCION, UnidadMedida.CAJA, 3, 10, "Estante B1"),
        Producto("P5", "MED-001", "Termómetro digital", "Termómetro digital de uso clínico", CategoriaInsumo.MEDICION, UnidadMedida.UNIDAD, 6, 5, "Vitrina C1"),
        Producto("P6", "MED-002", "Oxímetro de pulso", "Oxímetro de dedo con pantalla", CategoriaInsumo.MEDICION, UnidadMedida.UNIDAD, 0, 3, "Vitrina C1", EstadoProducto.AGOTADO),
        Producto("P7", "HIG-001", "Alcohol gel 500 ml", "Alcohol gel al 70%", CategoriaInsumo.HIGIENE, UnidadMedida.FRASCO, 25, 12, "Estante D2"),
        Producto("P8", "ORT-001", "Cuello ortopédico blando", "Talla única", CategoriaInsumo.ORTOPEDIA, UnidadMedida.UNIDAD, 4, 2, "Estante E1"),
    )

    private fun ingreso(id: String, productoId: String, fecha: String, cantidad: Int, observacion: String = "") =
        Movimiento(id, productoId, fecha, TipoMovimiento.INGRESO, cantidad, null, observacion, ENCARGADA, Perfil.ENCARGADO)

    private fun salida(
        id: String, productoId: String, fecha: String, cantidad: Int, motivo: MotivoSalida,
        observacion: String = "", vendedor: Boolean = true
    ) = Movimiento(
        id, productoId, fecha, TipoMovimiento.SALIDA, cantidad, motivo, observacion,
        if (vendedor) VENDEDOR else ENCARGADA,
        if (vendedor) Perfil.USUARIO_AUTORIZADO else Perfil.ENCARGADO
    )

    /** 11 movimientos de ejemplo (la "carga inicial" de cada producto más algunas salidas). */
    val movimientos = listOf(
        ingreso("M1", "P1", "2026-10-01 09:15", 50),
        salida("M2", "P1", "2026-10-02 16:40", 10, MotivoSalida.VENTA),
        ingreso("M3", "P2", "2026-10-01 10:00", 8, "Carga inicial"),
        ingreso("M4", "P3", "2026-10-01 10:15", 12, "Carga inicial"),
        ingreso("M5", "P4", "2026-10-01 10:30", 10, "Carga inicial"),
        salida("M6", "P4", "2026-10-03 11:05", 7, MotivoSalida.VENTA),
        ingreso("M7", "P5", "2026-10-01 11:00", 6, "Carga inicial"),
        ingreso("M8", "P6", "2026-10-01 11:20", 1, "Carga inicial"),
        salida("M9", "P6", "2026-10-04 10:30", 1, MotivoSalida.MERMA, "Pantalla dañada", vendedor = false),
        ingreso("M10", "P7", "2026-10-01 12:00", 25, "Carga inicial"),
        ingreso("M11", "P8", "2026-10-01 12:30", 4, "Carga inicial"),
    )
}
