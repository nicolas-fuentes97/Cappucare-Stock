package com.cappucare.app.data.repository

import com.cappucare.app.data.ModoOperacion
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.Usuario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Selector de fuente de datos: es la [FuenteDatos] que usa el ViewModel. En CADA operación consulta
 * el modo vigente ([ModoOperacion]) y delega en la fuente local o en la del servidor, así que
 * cambiar el modo tiene efecto inmediato (no solo al arrancar la app). `BuildConfig.USAR_API` solo
 * define el modo inicial, mientras el usuario no elija otro en Login > Servidor.
 *
 * Los Flow (catálogo e historial) también cambian de origen al cambiar el modo: la interfaz pasa a
 * mostrar los datos de la otra base sin reiniciar nada.
 *
 * Cada operación elige su fuente UNA sola vez al comenzar: si el modo cambia a mitad de una
 * escritura, esa escritura termina en la fuente donde empezó.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectorFuente(
    private val modo: ModoOperacion,
    private val local: FuenteDatos,
    private val servidor: FuenteDatos
) : FuenteDatos {

    /** Fuente que corresponde al modo vigente en este instante. */
    fun actual(): FuenteDatos = if (modo.esLocal) local else servidor

    private fun <T> segunModo(flujo: (FuenteDatos) -> Flow<T>): Flow<T> =
        modo.modoLocal.flatMapLatest { esLocal -> flujo(if (esLocal) local else servidor) }

    override val productos: Flow<List<Producto>> = segunModo { it.productos }
    override val movimientos: Flow<List<Movimiento>> = segunModo { it.movimientos }

    override suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin =
        actual().iniciarSesion(usuario, clave)

    override suspend fun cerrarSesion() = actual().cerrarSesion()

    override suspend fun sincronizar(): ApiResult<Unit> = actual().sincronizar()

    override suspend fun guardarProducto(producto: Producto): ApiResult<Producto> =
        actual().guardarProducto(producto)

    override suspend fun cambiarEstado(productoId: String, nuevoEstado: EstadoProducto): ApiResult<Producto> =
        actual().cambiarEstado(productoId, nuevoEstado)

    override suspend fun registrarMovimiento(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String,
        responsable: Usuario
    ): ApiResult<ResultadoMovimiento> =
        actual().registrarMovimiento(productoId, tipo, cantidad, motivo, observacion, responsable)
}
