package com.cappucare.app.data.repository

import com.cappucare.app.data.MockData
import com.cappucare.app.data.local.AlmacenLocal
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.remote.TipoError
import com.cappucare.app.model.ErroresMovimiento
import com.cappucare.app.model.ErroresProducto
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.Usuario
import com.cappucare.app.reglas.ReglasInventario
import com.cappucare.app.reglas.Seguridad
import com.cappucare.app.reglas.Validaciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * MODO LOCAL (respaldo para la defensa si no hay acceso al servicio): opera SOLO con la base Room
 * del equipo, sin red, y con las mismas reglas de negocio que el servidor. No duplica reglas: usa
 * las funciones puras de reglas/ (Validaciones, ReglasInventario, Seguridad), las mismas que
 * validan los formularios.
 *
 *  - Los errores salen con el mismo [TipoError] y el mismo campo que devuelve el servidor
 *    (por ejemplo CONFLICTO en "codigo" para un código repetido), así la interfaz los trata igual.
 *  - Es una base INDEPENDIENTE de la copia del servidor (otro archivo Room): lo que se crea aquí
 *    no se envía al servidor ni se sincroniza al volver al modo servidor.
 *  - Todo se guarda de inmediato (no hay cola de pendientes ni sincronización).
 *  - El permiso por perfil (RN-08) lo aplica el ViewModel antes de llamar, igual que en modo servidor.
 */
class FuenteLocal(
    private val almacen: AlmacenLocal,
    private val nuevoId: () -> String = { UUID.randomUUID().toString() },
    private val reloj: () -> String = { ReglasInventario.fechaActual() }
) : FuenteDatos {

    override val productos: Flow<List<Producto>> = almacen.productos
    override val movimientos: Flow<List<Movimiento>> = almacen.movimientos

    // ---------------------------------------------------------------- RF-00

    /** Login local: busca el usuario en Room y compara el SHA-256 con la misma sal que usa el servidor. */
    override suspend fun iniciarSesion(usuario: String, clave: String): ResultadoLogin {
        val encontrado = almacen.buscarUsuario(usuario.trim())
        return if (encontrado != null && Seguridad.verificar(clave, encontrado.claveHash)) {
            ResultadoLogin.Exito(encontrado, sinConexion = false)
        } else {
            ResultadoLogin.Rechazado(MENSAJE_CREDENCIALES)
        }
    }

    override suspend fun cerrarSesion() = Unit

    override suspend fun sincronizar(): ApiResult<Unit> = ApiResult.Success(Unit)

    // ---------------------------------------------------------------- RF-01 / RF-03

    /**
     * RF-01: crea (id vacío) o actualiza.
     *  - Nuevo: id único, cantidad 0 y estado DISPONIBLE (RN-01).
     *  - Editar: se conservan la cantidad y el estado que ya tenía.
     *  - El código se guarda en mayúsculas (RN-04: el índice único de Room no distingue cur-001 de CUR-001).
     */
    override suspend fun guardarProducto(producto: Producto): ApiResult<Producto> = protegido {
        val existentes = almacen.productosActuales()
        val esNuevo = producto.id.isBlank()
        val actual = if (esNuevo) null else existentes.firstOrNull { it.id == producto.id }
            ?: return@protegido error("El producto ya no existe", TipoError.NO_ENCONTRADO)

        val errores = Validaciones.validarProducto(
            idActual = if (esNuevo) null else producto.id,
            codigo = producto.codigo,
            nombre = producto.nombre,
            categoria = producto.categoria,
            unidadMedida = producto.unidadMedida,
            nivelMinimo = producto.nivelMinimo.toString(),
            ubicacion = producto.ubicacion,
            existentes = existentes
        )
        errorDeProducto(errores)?.let { return@protegido it }

        val limpio = producto.copy(
            codigo = producto.codigo.trim().uppercase(),
            nombre = producto.nombre.trim(),
            descripcion = producto.descripcion.trim(),
            ubicacion = producto.ubicacion.trim()
        )
        val guardado = if (actual == null) {
            limpio.copy(id = nuevoId(), cantidadDisponible = 0, estado = EstadoProducto.DISPONIBLE)
        } else {
            limpio.copy(cantidadDisponible = actual.cantidadDisponible, estado = actual.estado)
        }
        almacen.guardarProducto(guardado)
        ApiResult.Success(guardado)
    }

    /** RF-03: solo cambia el estado, y solo si es coherente con la cantidad (ReglasInventario.validarCambioEstado). */
    override suspend fun cambiarEstado(productoId: String, nuevoEstado: EstadoProducto): ApiResult<Producto> = protegido {
        val actual = almacen.productosActuales().firstOrNull { it.id == productoId }
            ?: return@protegido error("El producto ya no existe", TipoError.NO_ENCONTRADO)
        ReglasInventario.validarCambioEstado(actual, nuevoEstado)?.let {
            return@protegido error(it, TipoError.REGLA_DE_NEGOCIO, "estado")
        }
        val actualizado = actual.copy(estado = nuevoEstado)
        almacen.guardarProducto(actualizado)
        ApiResult.Success(actualizado)
    }

    // ---------------------------------------------------------------- RF-04 / RF-05 / RF-06 / RF-07

    /**
     * Guarda el movimiento y actualiza la cantidad en UNA sola transacción (RNF-03). Vuelve a
     * validar (RN-02, RN-03 y reglas de estado) con el valor real de la base: la pantalla no es la
     * única forma de llamar a esta función. Responsable = usuario con sesión; fecha = ahora (RN-05, RF-06).
     */
    override suspend fun registrarMovimiento(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String,
        responsable: Usuario
    ): ApiResult<ResultadoMovimiento> = protegido {
        almacen.enTransaccion<ApiResult<ResultadoMovimiento>> {
            val antes = almacen.productosActuales().firstOrNull { it.id == productoId }
                ?: return@enTransaccion error("El producto ya no existe", TipoError.NO_ENCONTRADO, "producto")
            errorDeMovimiento(Validaciones.validarMovimiento(antes, tipo, cantidad.toString(), motivo), tipo, cantidad, motivo)
                ?.let { return@enTransaccion it }

            val despues = ReglasInventario.aplicarMovimiento(antes, tipo, cantidad)   // RN-01, RN-03, RF-07
            val movimiento = Movimiento(
                id = nuevoId(),
                productoId = productoId,
                fecha = reloj(),                                                      // RN-05
                tipo = tipo,
                cantidad = cantidad,
                motivo = if (tipo == TipoMovimiento.SALIDA) motivo else null,         // RN-02
                observacion = observacion.trim(),
                responsable = responsable.nombre,                                     // RF-06
                responsablePerfil = responsable.perfil                                // RF-06
            )
            almacen.guardarMovimiento(movimiento, despues)                            // RF-07
            ApiResult.Success(ResultadoMovimiento(antes, despues))
        }
    }

    // ---------------------------------------------------------------- datos de ejemplo

    /**
     * "Restablecer datos locales": vuelve a los datos de ejemplo de MockData (8 productos y 11
     * movimientos). Solo toca la base local; la copia del servidor no se modifica.
     */
    suspend fun restablecerDatos() {
        almacen.reemplazarDatos(MockData.productos, MockData.movimientos)
    }

    // ---------------------------------------------------------------- errores como los del servidor

    /** Una falla inesperada de la base (por ejemplo el índice único de Room) se vuelve un error con mensaje, no un cierre de la app. */
    private suspend fun <T> protegido(bloque: suspend () -> ApiResult<T>): ApiResult<T> =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "No se pudo guardar en el equipo", tipo = TipoError.DESCONOCIDO)
        }

    private fun error(mensaje: String, tipo: TipoError, campo: String? = null) =
        ApiResult.Error(mensaje, codigoHttp = null, campo = campo, tipo = tipo)

    /** Primer error del formulario de producto, en el mismo orden de campos que revisa el servidor. */
    private fun errorDeProducto(e: ErroresProducto): ApiResult.Error? {
        e.codigo?.let {
            val tipo = if (it == Validaciones.MENSAJE_CODIGO_REPETIDO) TipoError.CONFLICTO else TipoError.SOLICITUD_INVALIDA
            return error(it, tipo, "codigo")
        }
        e.nombre?.let { return error(it, TipoError.SOLICITUD_INVALIDA, "nombre") }
        e.categoria?.let { return error(it, TipoError.SOLICITUD_INVALIDA, "categoria") }
        e.unidadMedida?.let { return error(it, TipoError.SOLICITUD_INVALIDA, "unidadMedida") }
        e.nivelMinimo?.let { return error(it, TipoError.SOLICITUD_INVALIDA, "nivelMinimo") }
        e.ubicacion?.let { return error(it, TipoError.SOLICITUD_INVALIDA, "ubicacion") }
        return null
    }

    /** Error de movimiento con el tipo que daría el servidor: dato inválido (400) o regla de negocio (422). */
    private fun errorDeMovimiento(
        e: ErroresMovimiento,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?
    ): ApiResult.Error? {
        if (!e.hayErrores) return null
        val cantidadInvalida = cantidad <= 0 || cantidad > Validaciones.CANTIDAD_MAXIMA
        val sinMotivo = tipo == TipoMovimiento.SALIDA && motivo == null
        e.cantidad?.takeIf { cantidadInvalida }?.let { return error(it, TipoError.SOLICITUD_INVALIDA, "cantidad") }
        e.motivo?.takeIf { sinMotivo }?.let { return error(it, TipoError.SOLICITUD_INVALIDA, "motivo") }
        e.producto?.let { return error(it, TipoError.REGLA_DE_NEGOCIO, "producto") }
        e.cantidad?.let { return error(it, TipoError.REGLA_DE_NEGOCIO, "cantidad") }
        e.motivo?.let { return error(it, TipoError.REGLA_DE_NEGOCIO, "motivo") }
        return null
    }

    companion object {
        const val MENSAJE_CREDENCIALES = "Usuario o clave incorrectos"
    }
}
