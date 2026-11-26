package com.cappucare.app.data.repository

import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio: la única interfaz de datos que conoce el ViewModel. Su implementación es
 * [FuenteLocal] (Room/SQLite en el equipo). Por ser una interfaz, en las pruebas del ViewModel se
 * puede reemplazar por un doble de prueba (MockK) sin cambiar el ViewModel.
 *
 * Las escrituras devuelven [ApiResult]: Success con el dato guardado o Error con el mensaje, el
 * tipo de error y el campo del formulario al que corresponde.
 */
interface FuenteDatos {

    /** Catálogo por nombre. */
    val productos: Flow<List<Producto>>

    /** Historial, del más reciente al más antiguo. */
    val movimientos: Flow<List<Movimiento>>

    /** RF-00. */
    suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin

    /** RF-01: crea (id vacío) o edita. */
    suspend fun guardarProducto(producto: Producto): ApiResult<Producto>

    /** RF-03. */
    suspend fun cambiarEstado(productoId: String, nuevoEstado: EstadoProducto): ApiResult<Producto>

    /** Eliminar (CRUD): solo productos SIN movimientos; con historial se dan de baja (RN-06). */
    suspend fun eliminarProducto(productoId: String): ApiResult<Producto>

    /** RF-04 / RF-05 / RF-06 / RF-07. */
    suspend fun registrarMovimiento(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String,
        responsable: Usuario
    ): ApiResult<ResultadoMovimiento>

    /** Vuelve a los datos de ejemplo (8 productos y 11 movimientos). */
    suspend fun restablecerDatos()
}
