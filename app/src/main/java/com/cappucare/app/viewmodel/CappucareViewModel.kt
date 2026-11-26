package com.cappucare.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cappucare.app.BuildConfig
import com.cappucare.app.data.ModoOperacion
import com.cappucare.app.data.local.AlmacenRoom
import com.cappucare.app.data.local.AppDatabase
import com.cappucare.app.data.local.PreferenciasModo
import com.cappucare.app.data.local.PreferenciasServidor
import com.cappucare.app.data.local.PreferenciasSesion
import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.remote.ErroresServidor
import com.cappucare.app.data.remote.FabricaApi
import com.cappucare.app.data.remote.FuenteRemota
import com.cappucare.app.data.remote.ManejoErrores
import com.cappucare.app.data.remote.PruebaServidor
import com.cappucare.app.data.remote.ResultadoPruebaServidor
import com.cappucare.app.data.remote.SesionRemota
import com.cappucare.app.data.remote.TipoError
import com.cappucare.app.data.repository.CappucareRepository
import com.cappucare.app.data.repository.FuenteLocal
import com.cappucare.app.data.repository.FuenteServidor
import com.cappucare.app.data.repository.ResultadoLogin
import com.cappucare.app.data.repository.SelectorFuente
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
import com.cappucare.app.reglas.ResultadoUrl
import com.cappucare.app.reglas.ResumenHistorial
import com.cappucare.app.reglas.UrlServidor
import com.cappucare.app.reglas.Validaciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel único, compartido por todas las pantallas (se crea una sola vez en AppNavigation).
 *
 *  - Expone el estado con StateFlow (las pantallas lo leen con collectAsState()).
 *  - Valida con Validaciones y aplica las reglas de negocio (reglas/) ANTES de guardar.
 *  - Lee y guarda a través del SelectorFuente (nunca toca Room ni Retrofit directamente), que en
 *    cada operación usa el modo vigente: MODO LOCAL (FuenteLocal: solo Room, respaldo sin servidor)
 *    o MODO SERVIDOR (FuenteServidor: la API que implementes en los TODO(E3-n), con Room como copia).
 *  - BuildConfig.USAR_API solo define el modo INICIAL; el usuario lo cambia en Login > Servidor
 *    (interruptor "Modo local") y la elección queda guardada.
 *  - Avisa resultados a la interfaz con "mensajes" (Snackbar) y "avisoStockMinimo" (notificación).
 *
 * Es AndroidViewModel porque necesita el Context de la aplicación para abrir la base de datos.
 */
class CappucareViewModel(application: Application) : AndroidViewModel(application) {

    // --- EA3: capa remota (se usa en modo servidor) ---
    private val sesionRemota = SesionRemota()
    private val fabricaApi = FabricaApi(sesionRemota)
    private val preferenciasServidor = PreferenciasServidor(application)
    private val preferenciasModo = PreferenciasModo(application)

    // --- Modo de operación (local / servidor): se puede cambiar en caliente ---
    /** Valor inicial: según USAR_API (false = local, true = servidor). La preferencia del usuario manda después. */
    private val modoOperacion = ModoOperacion(modoLocalInicial = !BuildConfig.USAR_API)
    private var modoElegidoEnEstaEjecucion = false

    /** true = modo local (solo Room, sin servidor); false = modo servidor. La interfaz lo observa. */
    val modoLocal: StateFlow<Boolean> = modoOperacion.modoLocal

    /** Dirección del servidor en uso: la elegida en Login > Servidor o, si no hay, la de BuildConfig. */
    private val urlPorDefecto: String =
        if (BuildConfig.USAR_API) {
            (UrlServidor.normalizar(BuildConfig.API_BASE_URL) as? ResultadoUrl.Valida)?.url ?: UrlServidor.URL_EMULADOR
        } else {
            BuildConfig.API_BASE_URL
        }
    private val _urlServidor = MutableStateFlow(urlPorDefecto)
    val urlServidor: StateFlow<String> = _urlServidor.asStateFlow()

    /** MODO LOCAL: base Room propia, independiente de la copia del servidor. */
    private val fuenteLocal = FuenteLocal(AlmacenRoom(AppDatabase.obtener(application, AppDatabase.NOMBRE_LOCAL)))

    /** MODO SERVIDOR: API como fuente de verdad (TODO E3-n); su copia vive en otra base Room. */
    private val repositorioServidor = CappucareRepository(
        db = AppDatabase.obtener(application, AppDatabase.NOMBRE_SERVIDOR),
        remota = FuenteRemota { fabricaApi.servicio(_urlServidor.value) },
        sesion = sesionRemota
    )

    /** Única fuente de datos que usa este ViewModel: elige local o servidor en cada operación. */
    private val fuentes = SelectorFuente(modoOperacion, fuenteLocal, FuenteServidor(repositorioServidor))
    private val preferencias = PreferenciasSesion(application)

    init {
        // Modo guardado por el usuario (si no eligió ninguno rige el de USAR_API). Cada operación
        // lee modoOperacion, que se actualiza aquí y en cambiarModoLocal().
        viewModelScope.launch {
            val guardado = try {
                preferenciasModo.modoLocalGuardado.first()
            } catch (e: IOException) {
                null
            }
            if (!modoElegidoEnEstaEjecucion) {
                modoOperacion.establecer(ModoOperacion.resolver(guardado, !BuildConfig.USAR_API))
            }
        }
        // Recupera la dirección que el usuario guardó en Login > Servidor.
        viewModelScope.launch {
            preferenciasServidor.urlGuardada.collect { guardada ->
                if (!guardada.isNullOrBlank()) _urlServidor.value = guardada
            }
        }
    }

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

    /** EA3: true mientras se espera la respuesta de POST /auth/login. */
    private val _cargandoLogin = MutableStateFlow(false)
    val cargandoLogin: StateFlow<Boolean> = _cargandoLogin.asStateFlow()

    /** EA3: true si se ingresó con los usuarios del equipo porque no había conexión con el servidor. */
    private val _sesionSinConexion = MutableStateFlow(false)
    val sesionSinConexion: StateFlow<Boolean> = _sesionSinConexion.asStateFlow()

    /** Último correo que inició sesión (DataStore), para rellenar el campo de usuario. */
    val ultimoUsuario: StateFlow<String?> =
        preferencias.ultimoUsuario.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun validarLogin(usuario: String, clave: String): ErroresLogin = Validaciones.validarLogin(usuario, clave)

    fun limpiarErrorLogin() {
        _errorLogin.value = null
    }

    /**
     * Valida los campos y luego inicia sesión con la fuente del modo vigente: en modo local busca el
     * usuario en Room y compara el SHA-256 de la clave (misma sal que el servidor); en modo servidor
     * ingresa con POST /auth/login (E3-7) y, sin red, con los usuarios guardados en el equipo. Si
     * las credenciales no coinciden deja un mensaje genérico (no dice cuál de los dos falló).
     * Cuando usuarioActivo deja de ser null, LoginScreen navega al Panel.
     */
    fun login(usuario: String, clave: String) {
        _errorLogin.value = null
        if (Validaciones.validarLogin(usuario, clave).hayErrores) return
        if (_cargandoLogin.value) return
        viewModelScope.launch {
            _cargandoLogin.value = true
            try {
                when (val r = fuentes.iniciarSesion(usuario, clave)) {
                    is ResultadoLogin.Exito -> {
                        try {
                            preferencias.guardarUltimoUsuario(r.usuario.usuario)
                        } catch (e: IOException) {
                            // recordar el usuario es opcional: si no se puede guardar, se ingresa igual
                        }
                        _sesionSinConexion.value = r.sinConexion
                        _sincronizacion.value = if (r.sinConexion) {
                            EstadoSincronizacion(
                                error = "Sin conexión con el servidor: se muestran los datos guardados en el equipo.",
                                sinConexion = true
                            )
                        } else {
                            EstadoSincronizacion()
                        }
                        _usuarioActivo.value = r.usuario
                        if (!r.sinConexion) sincronizar()
                    }
                    is ResultadoLogin.Rechazado -> _errorLogin.value = r.mensaje
                    is ResultadoLogin.Fallo -> _errorLogin.value = r.mensaje
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
        _sesionSinConexion.value = false
        _sincronizacion.value = EstadoSincronizacion()
        viewModelScope.launch { fuentes.cerrarSesion() }
    }

    /** La API respondió 401 con una sesión abierta: se vuelve al login (la guardia de AppNavigation navega). */
    private fun expirarSesion() {
        if (_usuarioActivo.value == null) return
        logout()
        avisar(ManejoErrores.MENSAJE_SESION_VENCIDA)
    }

    // =====================================================================
    // EA3 · Servidor: dirección configurable sin recompilar (Login > Servidor)
    // =====================================================================

    /** Mensaje de error de la dirección escrita, o null si es válida (para el campo del diálogo). */
    fun validarUrlServidor(texto: String): String? = UrlServidor.error(texto)

    /** "Probar conexión": GET /health contra la dirección escrita (sin guardarla). */
    fun probarServidor(texto: String, onResultado: (ResultadoPruebaServidor) -> Unit) {
        viewModelScope.launch {
            onResultado(PruebaServidor.probar(texto) { url -> fabricaApi.servicio(url) })
        }
    }

    /** Guarda la dirección (DataStore). [onFin] recibe el mensaje de error o null si quedó guardada. */
    fun guardarUrlServidor(texto: String, onFin: (String?) -> Unit) {
        when (val url = UrlServidor.normalizar(texto)) {
            is ResultadoUrl.Invalida -> onFin(url.mensaje)
            is ResultadoUrl.Valida -> viewModelScope.launch {
                try {
                    preferenciasServidor.guardar(url.url)
                    _urlServidor.value = url.url
                    onFin(null)
                } catch (e: IOException) {
                    onFin("No se pudo guardar la dirección en el equipo")
                }
            }
        }
    }

    /** Vuelve a la dirección de BuildConfig.API_BASE_URL. */
    fun restablecerUrlServidor(onFin: () -> Unit) {
        viewModelScope.launch {
            try {
                preferenciasServidor.restablecer()
            } catch (e: IOException) {
                // si no se puede borrar, al menos se usa la de fábrica en esta sesión
            }
            _urlServidor.value = urlPorDefecto
            onFin()
        }
    }

    // =====================================================================
    // Modo local / modo servidor (Login > Servidor)
    // =====================================================================

    /**
     * Cambia el modo en caliente y lo guarda (DataStore). La sesión pertenece al modo en que se
     * abrió: si hay un usuario con sesión, primero se cierra (en su modo) y se vuelve al login.
     * Los datos de cada modo son independientes: lo creado en modo local no se envía al servidor.
     */
    fun cambiarModoLocal(local: Boolean) {
        if (local == modoOperacion.esLocal) return
        modoElegidoEnEstaEjecucion = true
        viewModelScope.launch {
            if (_usuarioActivo.value != null) {
                _usuarioActivo.value = null
                _productoSeleccionadoId.value = null
                _sesionSinConexion.value = false
                fuentes.cerrarSesion()          // todavía en el modo anterior
            }
            _errorLogin.value = null
            _sincronizacion.value = EstadoSincronizacion()
            modoOperacion.establecer(local)
            avisar(if (local) MENSAJE_MODO_LOCAL else MENSAJE_MODO_SERVIDOR)
            try {
                preferenciasModo.guardar(local)
            } catch (e: IOException) {
                // si no se puede guardar, el modo vale igual durante esta ejecución
            }
        }
    }

    /** "Restablecer datos locales": vuelve la base del modo local a los datos de ejemplo. */
    fun restablecerDatosLocales(onFin: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                fuenteLocal.restablecerDatos()
                _productoSeleccionadoId.value = null
                avisar("Datos locales restablecidos a los de ejemplo")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                avisar("No se pudieron restablecer los datos locales")
            }
            onFin()
        }
    }

    // =====================================================================
    // EA3 · Sincronización con el servidor (API -> Room)
    // =====================================================================

    /** Estado visible de la sincronización: cargando, último error o cuándo se actualizó por última vez. */
    data class EstadoSincronizacion(
        val cargando: Boolean = false,
        val error: String? = null,
        val sinConexion: Boolean = false,
        val ultimaActualizacion: String? = null
    )

    private val _sincronizacion = MutableStateFlow(EstadoSincronizacion())
    val sincronizacion: StateFlow<EstadoSincronizacion> = _sincronizacion.asStateFlow()

    /** true mientras se guarda algo en el servidor (para deshabilitar botones y mostrar el progreso). */
    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    private var trabajoSincronizacion: Job? = null

    /** Renueva la copia local con lo que dice el servidor (E3-6a). En modo local no hay servidor: no hace nada. */
    fun sincronizar() {
        if (modoOperacion.esLocal || _usuarioActivo.value == null) return
        if (trabajoSincronizacion?.isActive == true) return
        trabajoSincronizacion = viewModelScope.launch {
            val anterior = _sincronizacion.value
            _sincronizacion.value = anterior.copy(cargando = true, error = null)
            when (val r = fuentes.sincronizar()) {
                is ApiResult.Success ->
                    _sincronizacion.value = EstadoSincronizacion(ultimaActualizacion = ReglasInventario.fechaActual())
                is ApiResult.SinConexion ->
                    _sincronizacion.value = anterior.copy(cargando = false, error = r.mensaje, sinConexion = true)
                is ApiResult.Error ->
                    if (r.tipo == TipoError.NO_AUTORIZADO && !_sesionSinConexion.value) {
                        expirarSesion()
                    } else {
                        _sincronizacion.value = anterior.copy(cargando = false, error = r.mensaje, sinConexion = false)
                    }
            }
        }
    }

    /**
     * Muestra al usuario el resultado de una ESCRITURA en la API: el éxito lo maneja [alExito]; la
     * falta de conexión y los errores del servidor se avisan en el Snackbar (con el campo si lo hay).
     * Un 401 con sesión abierta cierra la sesión.
     */
    private fun <T> resolver(resultado: ApiResult<T>, alExito: (T) -> Unit) {
        when (resultado) {
            is ApiResult.Success -> alExito(resultado.datos)
            is ApiResult.SinConexion -> avisar("${resultado.mensaje} No se guardó ningún cambio.")
            is ApiResult.Error ->
                if (resultado.tipo == TipoError.NO_AUTORIZADO && _sesionSinConexion.value) {
                    avisar("Ingresaste sin conexión: cierra sesión y vuelve a ingresar con el servidor encendido para guardar cambios.")
                } else if (resultado.tipo == TipoError.NO_AUTORIZADO) {
                    expirarSesion()
                } else if (ErroresServidor.campoDeProducto(resultado.campo) || ErroresServidor.campoDeMovimiento(resultado.campo)) {
                    avisar(ErroresServidor.mensajeConCampo(resultado.campo, resultado.mensaje))
                } else {
                    avisar(resultado.mensaje)
                }
        }
    }

    /** RN-08 (supuesto SUP-06): solo el Personal encargado crea, edita y cambia estados. */
    fun puedeEditarCatalogo(): Boolean = _usuarioActivo.value?.perfil?.puedeEditarCatalogo == true

    // =====================================================================
    // Gestión de productos (RF-01, RF-02, RF-03, RF-08)
    // =====================================================================

    /** Catálogo desde Room, ordenado por nombre. Eagerly: siempre al día mientras viva el ViewModel. */
    val productos: StateFlow<List<Producto>> =
        fuentes.productos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    /**
     * RF-01: crea o actualiza el producto con la fuente del modo vigente (la cantidad se conserva al
     * editar, RN-01). Modo local: Room con las reglas de reglas/. Modo servidor: la API decide (E3-6b)
     * y Room solo refleja lo aceptado.
     */
    fun guardarProducto(producto: Producto) {
        if (!puedeEditarCatalogo()) {
            avisar(MENSAJE_SIN_PERMISO)
            return
        }
        if (_ocupado.value) return
        viewModelScope.launch {
            _ocupado.value = true
            try {
                resolver(fuentes.guardarProducto(producto)) {
                    avisar(if (producto.id.isBlank()) "Producto \"${producto.nombre}\" creado" else "Cambios guardados")
                }
            } finally {
                _ocupado.value = false
            }
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
        if (_ocupado.value) return
        viewModelScope.launch {
            _ocupado.value = true
            try {
                resolver(fuentes.cambiarEstado(productoId, nuevoEstado)) {
                    avisar("Estado actualizado a ${nuevoEstado.etiqueta}")
                }
            } finally {
                _ocupado.value = false
            }
        }
    }

    // =====================================================================
    // Movimientos de inventario (RF-04, RF-05, RF-06, RF-07)
    // =====================================================================

    val movimientos: StateFlow<List<Movimiento>> =
        fuentes.movimientos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Errores por campo del formulario de movimiento (RN-02, RN-03 y reglas de estado). */
    fun validarMovimiento(
        productoId: String?,
        tipo: TipoMovimiento,
        cantidad: String,
        motivo: MotivoSalida?
    ): ErroresMovimiento = Validaciones.validarMovimiento(productoPorId(productoId), tipo, cantidad, motivo)

    /**
     * Registra el movimiento y actualiza la cantidad en una sola transacción (Repository).
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
        if (_ocupado.value) return
        viewModelScope.launch {
            _ocupado.value = true
            try {
                resolver(fuentes.registrarMovimiento(productoId, tipo, cantidad, motivo, observacion, usuario)) { resultado ->
                    val accion = if (tipo == TipoMovimiento.INGRESO) "Ingreso de $cantidad" else "Salida de $cantidad"
                    avisar("$accion registrado. ${resultado.despues.nombre}: ${resultado.despues.cantidadDisponible} disponibles")
                    // RNF-07: avisa solo cuando ESTE movimiento hace cruzar el nivel mínimo.
                    if (ReglaReposicion.cruzoElMinimo(resultado.antes, resultado.despues)) {
                        _avisoStockMinimo.tryEmit(resultado.despues)
                    }
                }
            } finally {
                _ocupado.value = false
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
        fuentes.productos
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

    private companion object {
        const val MENSAJE_MODO_LOCAL = "Modo local: la app usa solo los datos de este equipo (sin servidor)."
        const val MENSAJE_MODO_SERVIDOR = "Modo servidor: la app usa la API."
        const val MENSAJE_SIN_PERMISO = "No tienes permiso para realizar esta acción."
    }
}
