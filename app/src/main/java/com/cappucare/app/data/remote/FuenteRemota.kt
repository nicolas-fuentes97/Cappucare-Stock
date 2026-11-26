package com.cappucare.app.data.remote

import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento

/**
 * TODO(E3-5) · Fuente de datos remota.
 *
 * Es la única clase que habla con [ApiService]. Cada función llama a la ruta correspondiente,
 * convierte la respuesta con los mapeadores (E3-4) y devuelve un [ApiResult] ya traducido por
 * [ManejoErrores] (E3-3). El repositorio (E3-6) solo ve modelos y ApiResult.
 *
 * [api] entrega el servicio vigente en cada llamada (FabricaApi lo renueva si cambia la dirección
 * del servidor); NO guardes el ApiService en una propiedad.
 *
 * QUÉ HACER: implementar cada función con el patrón
 *     ManejoErrores.llamar { api().ruta(...).aModelo() }
 *  - login: `api().login(LoginRequestDto(usuario.trim(), clave)).aSesion()`.
 *  - productos / movimientos: listas completas; mapear cada elemento.
 *  - crearProducto / editarProducto / cambiarEstado: usar `aRequest()`.
 *  - registrarMovimiento: construir el cuerpo con `crearRequestMovimiento` y mapear la respuesta.
 *  - logout: `api().logout()` (POST /auth/logout).
 *  - health: devuelve el [HealthDto] tal cual (lo usa "Probar conexión").
 *
 * CRITERIO DE ACEPTACIÓN: con un `ApiService` falso (MockK) que responde bien, cada función
 * devuelve Success con el modelo esperado; si el ApiService lanza HttpException(409) devuelve
 * Error(tipo = CONFLICTO) sin lanzar excepciones (pruebas E3-11).
 */
class FuenteRemota(private val api: () -> ApiService) {

    suspend fun login(usuario: String, clave: String): ApiResult<SesionRemotaDatos> {
        TODO("E3-5: ManejoErrores.llamar { api().login(LoginRequestDto(...)).aSesion() }")
    }

    suspend fun logout(): ApiResult<OkDto> {
        TODO("E3-5: ManejoErrores.llamar { api().logout() }")
    }

    suspend fun health(): ApiResult<HealthDto> {
        TODO("E3-5: ManejoErrores.llamar { api().health() }")
    }

    suspend fun productos(): ApiResult<List<Producto>> {
        TODO("E3-5: GET /productos mapeado a List<Producto>")
    }

    suspend fun movimientos(): ApiResult<List<Movimiento>> {
        TODO("E3-5: GET /movimientos mapeado a List<Movimiento>")
    }

    suspend fun crearProducto(producto: Producto): ApiResult<Producto> {
        TODO("E3-5: POST /productos con producto.aRequest()")
    }

    suspend fun editarProducto(producto: Producto): ApiResult<Producto> {
        TODO("E3-5: PUT /productos/{id} con producto.aRequest()")
    }

    suspend fun cambiarEstado(productoId: String, estado: EstadoProducto): ApiResult<Producto> {
        TODO("E3-5: PATCH /productos/{id}/estado con estado.aRequest()")
    }

    suspend fun registrarMovimiento(
        idCliente: String,
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String
    ): ApiResult<RespuestaMovimiento> {
        TODO("E3-5: POST /movimientos con crearRequestMovimiento(...) y mapear la respuesta")
    }
}
