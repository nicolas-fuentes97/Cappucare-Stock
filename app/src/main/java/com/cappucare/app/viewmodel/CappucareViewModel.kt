package com.cappucare.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cappucare.app.data.local.AlmacenRoom
import com.cappucare.app.data.local.AppDatabase
import com.cappucare.app.data.local.PreferenciasSesion
import com.cappucare.app.data.local.PreferenciasUsuario
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.repository.FuenteDatos
import com.cappucare.app.data.repository.FuenteLocal
import com.cappucare.app.data.repository.ResultadoLogin
import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.ErroresLogin
import com.cappucare.app.model.ErroresMovimiento
import com.cappucare.app.model.ErroresProducto
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import com.cappucare.app.model.Usuario
import com.cappucare.app.reglas.MovimientoConSaldo
import com.cappucare.app.reglas.ReglaReposicion
import com.cappucare.app.reglas.ReglasHistorial
import com.cappucare.app.reglas.ReglasInventario
import com.cappucare.app.reglas.ResumenHistorial
import com.cappucare.app.reglas.Validaciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel único, compartido por todas las pantallas (se crea una sola vez en AppNavigation).
 *
 *  - Expone el estado con StateFlow (las pantallas lo leen con collectAsState()).
 *  - Valida con Validaciones y aplica las reglas de negocio (reglas/) ANTES de guardar.
 *  - Lee y guarda a través del repositorio [FuenteDatos] (nunca toca Room directamente). En la app
 *    es [FuenteLocal] sobre Room (SQLite del equipo).
 *  - Avisa resultados a la interfaz con "mensajes" (Snackbar) y "avisoStockMinimo" (notificación).
 *
 * Es AndroidViewModel porque necesita el Context de la aplicación para abrir la base de datos.
 * El repositorio y las preferencias llegan por el constructor (con valores por defecto para la
 * app): así las pruebas unitarias pueden entregar dobles de prueba (MockK o una base en memoria).
 * @JvmOverloads genera el constructor (Application) que usa la fábrica de ViewModels de Android.
 */
class CappucareViewModel @JvmOverloads constructor(
    application: Application,
    private val fuente: FuenteDatos = FuenteLocal(AlmacenRoom(AppDatabase.obtener(application))),
    private val preferencias: PreferenciasUsuario = PreferenciasSesion(application)
) : AndroidViewModel(application) {

    // =====================================================================
    // Mensajes para el usuario (Snackbar en AppNavigation)
    // =====================================================================

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    fun avisar(texto: String) {
        _mensajes.tryEmit(texto)
    }

    // =====================================================================
    // RF-00 · Inicio de sesión
    // =====================================================================

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _errorLogin = MutableStateFlow<String?>(null)
    val errorLogin: StateFlow<String?> = _errorLogin.asStateFlow()

    /** true mientras se verifica el usuario (el botón Ingresar muestra un indicador). */
    private val _cargandoLogin = MutableStateFlow(false)
    val cargandoLogin: StateFlow<Boolean> = _cargandoLogin.asStateFlow()

    /** Último correo que inició sesión (DataStore), para rellenar el campo de usuario. */
    val ultimoUsuario: StateFlow<String?> =
        preferencias.ultimoUsuario.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun validarLogin(usuario: String, clave: String): ErroresLogin = Validaciones.validarLogin(usuario, clave)

    fun limpiarErrorLogin() {
        _errorLogin.value = null
    }

    /**
     * Valida los campos y luego busca el usuario en Room y compara el SHA-256 de la clave. Si las
     * credenciales no coinciden deja un mensaje genérico (no dice cuál de los dos falló).
     * Cuando usuarioActivo deja de ser null, LoginScreen navega al Panel.
     */
    fun login(usuario: String, clave: String) {
        _errorLogin.value = null
        if (Validaciones.validarLogin(usuario, clave).hayErrores) return
        if (_cargandoLogin.value) return
        viewModelScope.launch {
            _cargandoLogin.value = true
            try {
                when (val r = fuente.iniciarSesion(usuario, clave)) {
                    is ResultadoLogin.Exito -> {
                        try {
                            preferencias.guardarUltimoUsuario(r.usuario.usuario)
                        } catch (e: IOException) {
                            // recordar el usuario es opcional: si no se puede guardar, se ingresa igual
                        }
                        _usuarioActivo.value = r.usuario
                    }
                    is ResultadoLogin.Rechazado -> _errorLogin.value = r.mensaje
                }
            } finally {
                _cargandoLogin.value = false
            }
        }
    }

    fun logout() {
        _usuarioActivo.value = null
        _errorLogin.value = null
        _productoSeleccionadoId.value = null
    }

    /** "Restablecer datos de ejemplo" (Login): vuelve la base a los 8 productos y 11 movimientos. */
    fun restablecerDatos(onFin: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                fuente.restablecerDatos()
                _productoSeleccionadoId.value = null
                avisar(MENSAJE_DATOS_RESTABLECIDOS)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                avisar("No se pudieron restablecer los datos")
            }
            onFin()
        }
    }

    // =====================================================================
    // Resultado de las escrituras
    // =====================================================================

    /** true mientras se guarda algo (para no repetir la operación con dos toques seguidos). */
    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    /** Ejecuta una escritura: marca "ocupado", llama al repositorio y avisa el resultado. */
    private fun <T> escribir(operacion: suspend () -> ApiResult<T>, alExito: (T) -> Unit) {
        if (_ocupado.value) return
        viewModelScope.launch {
            _ocupado.value = true
            try {
                when (val r = operacion()) {
                    is ApiResult.Success -> alExito(r.datos)
                    is ApiResult.Error -> avisar(r.mensaje)
                    is ApiResult.SinConexion -> avisar(r.mensaje)
                }
            } finally {
                _ocupado.value = false
            }
        }
    }

    /** RN-08 (supuesto SUP-06): solo el Personal encargado crea, edita, cambia estados y elimina. */
    fun puedeEditarCatalogo(): Boolean = _usuarioActivo.value?.perfil?.puedeEditarCatalogo == true

    // =====================================================================
    // Gestión de productos (RF-01, RF-02, RF-03, RF-08)
    // =====================================================================

    /** Catálogo desde Room, ordenado por nombre. Eagerly: siempre al día mientras viva el ViewModel. */
    val productos: StateFlow<List<Producto>> =
        fuente.productos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Producto elegido en una lista (para abrir su detalle, editarlo o ver su historial). */
    private val _productoSeleccionadoId = MutableStateFlow<String?>(null)
    val productoSeleccionadoId: StateFlow<String?> = _productoSeleccionadoId.asStateFlow()

    fun seleccionarProducto(id: String?) {
        _productoSeleccionadoId.value = id
    }

    fun productoPorId(id: String?): Producto? = productos.value.find { it.id == id }

    /** RNF-06: producto con el código leído por la cámara (null si no existe). */
    fun productoPorCodigo(codigo: String): Producto? = ReglasInventario.buscarPorCodigo(productos.value, codigo)

    /** RF-08 + RF-02: búsqueda por nombre o código y filtro por categoría. */
    fun buscarProductos(texto: String, categoria: CategoriaInsumo?): List<Producto> =
        ReglasInventario.filtrarProductos(productos.value, texto, categoria)

    /** RF-01: errores por campo del formulario de producto (incluye RN-04, código único). */
    fun validarProducto(
        idActual: String?,
        codigo: String,
        nombre: String,
        categoria: CategoriaInsumo?,
        unidadMedida: UnidadMedida?,
        nivelMinimo: String,
        ubicacion: String
    ): ErroresProducto = Validaciones.validarProducto(
        idActual, codigo, nombre, categoria, unidadMedida, nivelMinimo, ubicacion, productos.value
    )

    /** RF-01: crea o actualiza el producto (la cantidad se conserva al editar, RN-01). */
    fun guardarProducto(producto: Producto) {
        if (!puedeEditarCatalogo()) {
            avisar(MENSAJE_SIN_PERMISO)
            return
        }
        escribir({ fuente.guardarProducto(producto) }) {
            avisar(if (producto.id.isBlank()) "Producto \"${producto.nombre}\" creado" else "Cambios guardados")
        }
    }

    /** RF-03: cambia el estado. Si es el mismo que ya tiene (o no es coherente con la cantidad), avisa y no cambia. */
    fun cambiarEstado(productoId: String, nuevoEstado: EstadoProducto) {
        if (!puedeEditarCatalogo()) {
            avisar(MENSAJE_SIN_PERMISO)
            return
        }
        val producto = productoPorId(productoId) ?: return
        val error = ReglasInventario.validarCambioEstado(producto, nuevoEstado)
        if (error != null) {
            avisar(error)
            return
        }
        escribir({ fuente.cambiarEstado(productoId, nuevoEstado) }) {
            avisar("Estado actualizado a ${nuevoEstado.etiqueta}")
        }
    }

    /** true si el producto no tiene movimientos (solo esos se pueden eliminar). */
    fun puedeEliminar(productoId: String): Boolean = movimientos.value.none { it.productoId == productoId }

    /**
     * Eliminar (CRUD): solo el Personal encargado y solo productos SIN movimientos. El repositorio
     * vuelve a revisar la regla contra la base. [onEliminado] se llama cuando quedó borrado (la
     * pantalla de detalle vuelve a la lista).
     */
    fun eliminarProducto(productoId: String, onEliminado: () -> Unit = {}) {
        if (!puedeEditarCatalogo()) {
            avisar(MENSAJE_SIN_PERMISO)
            return
        }
        escribir({ fuente.eliminarProducto(productoId) }) { eliminado ->
            if (_productoSeleccionadoId.value == productoId) _productoSeleccionadoId.value = null
            avisar("Producto \"${eliminado.nombre}\" eliminado")
            onEliminado()
        }
    }

    // =====================================================================
    // Movimientos de inventario (RF-04, RF-05, RF-06, RF-07)
    // =====================================================================

    val movimientos: StateFlow<List<Movimiento>> =
        fuente.movimientos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Errores por campo del formulario de movimiento (RN-02, RN-03 y reglas de estado). */
    fun validarMovimiento(
        productoId: String?,
        tipo: TipoMovimiento,
        cantidad: String,
        motivo: MotivoSalida?
    ): ErroresMovimiento = Validaciones.validarMovimiento(productoPorId(productoId), tipo, cantidad, motivo)

    /**
     * Registra el movimiento y actualiza la cantidad en una sola transacción (repositorio).
     * RN-05 / RF-06: responsable = usuario con sesión iniciada; fecha = ahora (no se escriben a mano).
     */
    fun registrarMovimiento(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: MotivoSalida?,
        observacion: String
    ) {
        val usuario = _usuarioActivo.value
        if (usuario == null) {
            avisar("Debes iniciar sesión para registrar movimientos.")
            return
        }
        escribir({ fuente.registrarMovimiento(productoId, tipo, cantidad, motivo, observacion, usuario) }) { resultado ->
            val accion = if (tipo == TipoMovimiento.INGRESO) "Ingreso de $cantidad" else "Salida de $cantidad"
            avisar("$accion registrado. ${resultado.despues.nombre}: ${resultado.despues.cantidadDisponible} disponibles")
            // RNF-07: avisa solo cuando ESTE movimiento hace cruzar el nivel mínimo.
            if (ReglaReposicion.cruzoElMinimo(resultado.antes, resultado.despues)) {
                _avisoStockMinimo.tryEmit(resultado.despues)
            }
        }
    }

    // =====================================================================
    // Consulta y reposición (RF-09, RF-10)
    // =====================================================================

    /**
     * RF-10: movimientos de un producto (null = todos), del más reciente al más antiguo, cada uno
     * con el saldo que dejó. [tipo] (opcional) muestra solo ingresos o solo salidas.
     */
    fun historialDe(productoId: String?, tipo: TipoMovimiento? = null): List<MovimientoConSaldo> =
        ReglasHistorial.conSaldo(movimientos.value, productos.value, productoId, tipo)

    /** RF-10: total de unidades ingresadas y retiradas de un producto (null = todos). */
    fun resumenHistorial(productoId: String?): ResumenHistorial =
        ReglasHistorial.resumen(movimientos.value, productoId)

    /** RF-10: contenido CSV del historial que se está viendo (para exportarlo con el selector de archivos). */
    fun csvHistorial(items: List<MovimientoConSaldo>): String = ReglasHistorial.aCsv(items, productos.value)

    /**
     * RF-09: productos cuya cantidad disponible es MENOR que su nivel mínimo (sin los dados de
     * baja), ordenados por mayor falta (RN-07). Se calcula con ReglaReposicion sobre el catálogo
     * de Room: ReposicionScreen (lista) y PanelScreen (contador) lo leen con collectAsState() y
     * se actualizan solos cuando un movimiento cambia el stock.
     */
    val alertasReposicion: StateFlow<List<Producto>> =
        fuente.productos
            .map { ReglaReposicion.filtrarBajoMinimo(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** RN-07: misma comparación para marcar el producto en Productos y en el Detalle. */
    fun estaBajoMinimo(producto: Producto): Boolean = ReglaReposicion.estaBajoMinimo(producto)

    /** Unidades que faltan para llegar al nivel mínimo (0 si no falta nada). */
    fun faltaParaMinimo(producto: Producto): Int = ReglaReposicion.faltaParaMinimo(producto)

    /** Fracción disponible / mínimo (0..1) para la barra de cobertura de Reposición. */
    fun nivelDeCobertura(producto: Producto): Float = ReglaReposicion.nivelDeCobertura(producto)

    /**
     * RNF-07 (opcional): evento "este producto acaba de quedar bajo su mínimo".
     * El ViewModel NO muestra la notificación (no debe manejar vistas ni permisos): solo avisa.
     * La UI lo escucha en AppNavigation (EscuchaAvisosStock) y llama a NotificadorStock.
     */
    private val _avisoStockMinimo = MutableSharedFlow<Producto>(extraBufferCapacity = 16)
    val avisoStockMinimo: SharedFlow<Producto> = _avisoStockMinimo.asSharedFlow()

    companion object {
        const val MENSAJE_SIN_PERMISO = "No tienes permiso para realizar esta acción."
        const val MENSAJE_DATOS_RESTABLECIDOS = "Datos de ejemplo restablecidos"
    }
}
