package com.cappucare.app.data.repository

import androidx.room.withTransaction
import com.cappucare.app.data.local.AppDatabase
import com.cappucare.app.data.local.aEntidad
import com.cappucare.app.data.local.aModelo
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.remote.FuenteRemota
import com.cappucare.app.data.remote.SesionRemota
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * REPOSITORY del MODO SERVIDOR (EA3): la API es la fuente de verdad y Room (su propio archivo,
 * "cappucare.db") es la copia local que observa la interfaz. La integración con la API REST se
 * hace aquí, sin tocar las pantallas (RNF-05).
 *
 * El MODO LOCAL (respaldo sin servidor, solo Room y las mismas reglas de negocio) vive en
 * [com.cappucare.app.data.repository.FuenteLocal]; el ViewModel no elige entre ambos: usa el
 * [SelectorFuente], que en cada operación mira el modo vigente (ver ModoOperacion).
 *
 * Capas:  Pantalla (Compose) → CappucareViewModel → SelectorFuente ─┬→ FuenteLocal → Room (modo local)
 *                                                                   └→ FuenteServidor → CappucareRepository
 *                                                                        └→ FuenteRemota → ApiService (Retrofit) → API
 *                                                                        └→ DAO → Room (copia del servidor)
 */
class CappucareRepository(
    private val db: AppDatabase,
    /** EA3: fuente remota (la única que habla con ApiService). */
    private val remota: FuenteRemota? = null,
    /** EA3: sesión con el servidor (token). */
    private val sesion: SesionRemota? = null
) {

    private val usuarioDao = db.usuarioDao()
    private val productoDao = db.productoDao()
    private val movimientoDao = db.movimientoDao()

    /** Catálogo completo por nombre; Room vuelve a emitir cada vez que cambia la tabla productos. */
    val productos: Flow<List<Producto>> =
        productoDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    /** Todos los movimientos, del más reciente al más antiguo. */
    val movimientos: Flow<List<Movimiento>> =
        movimientoDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    /** RF-00 sin conexión: usuario guardado en el equipo, por su correo (sin distinguir mayúsculas). */
    suspend fun buscarUsuario(usuario: String): Usuario? =
        usuarioDao.buscarPorUsuario(usuario.trim())?.aModelo()

    // =====================================================================================
    // EA3 · Integración con la API (se usa en MODO SERVIDOR)
    //
    // Regla de oro: la API es la FUENTE DE VERDAD y Room es una COPIA LOCAL (caché).
    //   - Lectura: la API responde -> se actualiza Room -> la pantalla (que observa Room) se
    //     refresca sola. Si no hay conexión, la pantalla sigue mostrando lo último guardado.
    //   - Escritura: se envía primero a la API; solo si el servidor la acepta se refleja en Room.
    //     Sin conexión NO se guarda nada y se devuelve el error (mensaje claro, sin datos falsos).
    // Lo que ya viene resuelto (flujos de arriba) no se toca.
    // =====================================================================================

    /**
     * TODO(E3-6a) · Renueva la copia local con lo que dice el servidor (lectura).
     *
     * QUÉ HACER:
     *  1. Pedir `remota!!.productos()` y `remota.movimientos()`. Si alguna NO es Success, devolver ese
     *     mismo resultado de error (o SinConexion) SIN tocar Room: la copia anterior se conserva.
     *  2. Si ambas son Success, en UNA transacción (`db.withTransaction`): borrar de Room lo que el
     *     servidor ya no tiene (`borrarQueNoEstan` de movimientos y luego de productos, por la
     *     clave foránea) y guardar lo recibido (`guardarVarios`, con `aEntidad()`; productos antes
     *     que movimientos).
     *  3. Devolver `ApiResult.Success(Unit)`.
     *
     * CRITERIO DE ACEPTACIÓN: con el servidor encendido, tras iniciar sesión el Panel muestra los
     * productos y movimientos del servidor; con el servidor apagado muestra lo último guardado y
     * la app informa "sin conexión" (no se cierra ni se vacía).
     */
    suspend fun sincronizar(): ApiResult<Unit> {
        TODO("E3-6a: productos + movimientos de la API -> reemplazar la copia local en una transacción")
    }

    /**
     * TODO(E3-6b) · RF-01 por la API: crea (id vacío -> `remota.crearProducto`) o edita
     * (`remota.editarProducto`). Si el servidor responde Success, guardar el producto devuelto en
     * Room (`productoDao.guardar(producto.aEntidad())`) y devolverlo; en cualquier otro caso
     * devolver el resultado tal cual (el servidor es quien decide el 409 por código repetido).
     * CRITERIO: crear "CUR-001" (ya existe) deja la lista igual y devuelve Error(tipo = CONFLICTO).
     */
    suspend fun guardarProductoRemoto(producto: Producto): ApiResult<Producto> {
        TODO("E3-6b: crear o editar en la API y, solo si fue aceptado, reflejarlo en Room")
    }

    /**
     * TODO(E3-6c) · RF-03 por la API: `remota.cambiarEstado(...)`; si fue aceptado, guardar el
     * producto devuelto en Room. CRITERIO: pasar a "Agotado" un producto con unidades devuelve
     * Error(tipo = REGLA_DE_NEGOCIO) con el mensaje del servidor y Room no cambia.
     */
    suspend fun cambiarEstadoRemoto(productoId: String, nuevoEstado: EstadoProducto): ApiResult<Producto> {
        TODO("E3-6c: PATCH del estado y reflejar el producto devuelto en Room")
    }

    /**
     * TODO(E3-6d) · RF-04/05/06/07 por la API: `remota.registrarMovimiento(idCliente, ...)` con un
     * `UUID.randomUUID().toString()` como idCliente. Si fue aceptado, en UNA transacción guardar en
     * Room el movimiento y el producto con su nueva cantidad (vienen en la respuesta) y devolver
     * `ResultadoMovimiento(antes, despues)`. El responsable lo pone el servidor desde el token.
     * CRITERIO: una salida de más unidades que el stock devuelve Error(REGLA_DE_NEGOCIO, 422) y no
     * cambia Room; un ingreso correcto sube la cantidad en Room y en el servidor.
     */
    suspend fun registrarMovimientoRemoto(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String
    ): ApiResult<ResultadoMovimiento> {
        TODO("E3-6d: POST /movimientos y reflejar movimiento + producto en Room (una transacción)")
    }

    /**
     * TODO(E3-7a) · RF-00 contra la API: inicia sesión con `POST /auth/login`.
     *
     * QUÉ HACER:
     *  - `remota!!.login(usuario, clave)` Success -> `sesion!!.abrir(token, usuario.nombre)` y
     *    `ResultadoLogin.Exito(usuario)`.
     *  - Error 401 (TipoError.NO_AUTORIZADO) -> `ResultadoLogin.Rechazado("Usuario o clave incorrectos")`
     *    (mensaje genérico, no dice cuál de los dos falló).
     *  - SinConexion -> intentar con los usuarios guardados en el equipo: [buscarUsuario] +
     *    `Seguridad.verificar(clave, claveHash)`. Si coinciden: `sesion.abrir(null, nombre)` y
     *    `Exito(usuario, sinConexion = true)` (solo lectura de la copia local: sin token no se
     *    puede escribir). Si no coinciden: `Fallo(mensaje de sin conexión)`.
     *  - Cualquier otro Error -> `Fallo(mensaje)`.
     *
     * CRITERIO DE ACEPTACIÓN: encargado@cappucare.cl / encargado123 entra con el servidor
     * encendido; con clave mala muestra "Usuario o clave incorrectos"; con el servidor apagado
     * entra en modo sin conexión y la app lo avisa.
     */
    suspend fun iniciarSesionRemota(usuario: String, clave: String): ResultadoLogin {
        TODO("E3-7a: login contra /auth/login con respaldo local sin conexión")
    }

    /**
     * TODO(E3-7b) · Cierra la sesión: si hay token, avisar al servidor (`POST /auth/logout`, sin
     * fallar si no hay conexión) y luego `sesion.cerrar(tokenAnterior)`. Siempre debe quedar la
     * sesión local cerrada, haya o no respuesta del servidor.
     * (Usa `remota.logout()` de E3-5.)
     */
    suspend fun cerrarSesionRemota() {
        TODO("E3-7b: avisar logout al servidor (best effort) y limpiar el token local")
    }
}
