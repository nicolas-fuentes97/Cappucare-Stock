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
 * MODO SERVIDOR: adapta [CappucareRepository] (la integración con la API que resuelves en los
 * TODO(E3-n)) a la interfaz [FuenteDatos] que usa el ViewModel. No agrega lógica: cada función solo
 * llama a la del repositorio. Si una de ellas aún tiene su TODO sin resolver, fallará aquí como antes.
 */
class FuenteServidor(private val repositorio: CappucareRepository) : FuenteDatos {

    override val productos: Flow<List<Producto>> = repositorio.productos
    override val movimientos: Flow<List<Movimiento>> = repositorio.movimientos

    override suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin =
        repositorio.iniciarSesionRemota(usuario, clave)

    override suspend fun cerrarSesion() = repositorio.cerrarSesionRemota()

    override suspend fun sincronizar(): ApiResult<Unit> = repositorio.sincronizar()

    override suspend fun guardarProducto(producto: Producto): ApiResult<Producto> =
        repositorio.guardarProductoRemoto(producto)

    override suspend fun cambiarEstado(productoId: String, nuevoEstado: EstadoProducto): ApiResult<Producto> =
        repositorio.cambiarEstadoRemoto(productoId, nuevoEstado)

    /** [responsable] no se envía: lo pone el servidor a partir del token (RF-06). */
    override suspend fun registrarMovimiento(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String,
        responsable: Usuario
    ): ApiResult<ResultadoMovimiento> =
        repositorio.registrarMovimientoRemoto(productoId, tipo, cantidad, motivo, observacion)
}
