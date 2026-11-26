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
 * Única interfaz de datos que conoce el ViewModel. Tiene tres implementaciones:
 *
 *  - [FuenteLocal]: MODO LOCAL (solo Room, sin red; mismas reglas de negocio que el servidor).
 *  - [FuenteServidor]: MODO SERVIDOR (CappucareRepository: API como fuente de verdad + copia en Room).
 *  - [SelectorFuente]: la que usa el ViewModel; en cada operación delega en una de las dos según el
 *    modo vigente (ModoOperacion), así el cambio de modo es inmediato.
 *
 * Las dos primeras responden igual (ApiResult con el mismo tipo de error para la misma regla),
 * por lo que el ViewModel y las pantallas no distinguen de dónde vienen los datos.
 */
interface FuenteDatos {

    /** Catálogo por nombre. */
    val productos: Flow<List<Producto>>

    /** Historial, del más reciente al más antiguo. */
    val movimientos: Flow<List<Movimiento>>

    /** RF-00. */
    suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin

    suspend fun cerrarSesion()

    /** Renueva la copia desde el servidor (en modo local no hay nada que sincronizar). */
    suspend fun sincronizar(): ApiResult<Unit>

    /** RF-01: crea (id vacío) o edita. */
    suspend fun guardarProducto(producto: Producto): ApiResult<Producto>

    /** RF-03. */
    suspend fun cambiarEstado(productoId: String, nuevoEstado: EstadoProducto): ApiResult<Producto>

    /** RF-04 / RF-05 / RF-06 / RF-07 (en modo servidor el responsable lo pone el servidor desde el token). */
    suspend fun registrarMovimiento(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String,
        responsable: Usuario
    ): ApiResult<ResultadoMovimiento>
}
