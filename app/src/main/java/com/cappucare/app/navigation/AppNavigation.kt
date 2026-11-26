package com.cappucare.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cappucare.app.notificacion.EscuchaAvisosStock
import com.cappucare.app.ui.components.FranjaModo
import com.cappucare.app.ui.screen.LoginScreen
import com.cappucare.app.ui.screen.PanelScreen
import com.cappucare.app.ui.screen.movimientos.HistorialMovimientosScreen
import com.cappucare.app.ui.screen.movimientos.RegistrarMovimientoScreen
import com.cappucare.app.ui.screen.productos.ProductoDetalleScreen
import com.cappucare.app.ui.screen.productos.ProductoFormScreen
import com.cappucare.app.ui.screen.productos.ProductosScreen
import com.cappucare.app.ui.screen.reposicion.ReposicionScreen
import com.cappucare.app.viewmodel.CappucareViewModel

object Rutas {
    const val LOGIN = "login"
    const val PANEL = "panel"
    const val PRODUCTOS = "productos"
    const val PRODUCTO_FORM = "producto_form"
    const val PRODUCTO_DETALLE = "producto_detalle"
    const val MOVIMIENTO = "movimiento"
    const val HISTORIAL = "historial"
    const val REPOSICION = "reposicion"
}

/**
 * Flujo de pantallas (un solo NavHost y un único CappucareViewModel compartido):
 *
 *   LOGIN ──(login OK)──> PANEL
 *   PANEL ──> PRODUCTOS ──> PRODUCTO_DETALLE ──> PRODUCTO_FORM (editar)
 *                       └─> PRODUCTO_FORM (nuevo)      ├─> MOVIMIENTO
 *                                                      └─> HISTORIAL (de ese producto)
 *   PANEL ──> MOVIMIENTO | HISTORIAL (todos) | REPOSICION ──> PRODUCTO_DETALLE / MOVIMIENTO
 *
 * El producto elegido se comparte a través del ViewModel (seleccionarProducto): no se pasan
 * argumentos por la ruta.
 *
 * Botón atrás: el atrás nativo de Android funciona solo (lo maneja el NavHost). Al ingresar,
 * LOGIN se saca de la pila; "Cerrar sesión" limpia la pila completa y vuelve a LOGIN (RNF-08).
 * Si el sistema restaura la app sin sesión (por ejemplo, tras cerrar el proceso), cualquier
 * pantalla distinta de LOGIN vuelve al login.
 *
 * Animaciones: cada pantalla entra deslizándose y la anterior se desvanece (y al volver, al revés).
 * Mensajes: los avisos del ViewModel ("Producto creado", "Ingreso registrado"...) se muestran en
 * un Snackbar común a todas las pantallas.
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: CappucareViewModel = viewModel()
) {
    val estadoSnackbar = remember { SnackbarHostState() }

    // Retroalimentación: cada mensaje del ViewModel se muestra en el Snackbar.
    LaunchedEffect(viewModel) {
        viewModel.mensajes.collect { texto -> estadoSnackbar.showSnackbar(texto) }
    }

    // RF-09 / RNF-07: notificación cuando un producto queda bajo su mínimo (en cualquier pantalla).
    EscuchaAvisosStock(viewModel)

    // RNF-08: sin sesión solo se puede estar en el login. Cubre "Cerrar sesión" (el ViewModel
    // deja usuarioActivo en null) y la restauración de la app sin sesión.
    val usuario by viewModel.usuarioActivo.collectAsState()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    LaunchedEffect(usuario, rutaActual) {
        if (usuario == null && rutaActual != null && rutaActual != Rutas.LOGIN) {
            navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    val cerrarSesion: () -> Unit = { viewModel.logout() }
    val irAlPanel: () -> Unit = {
        navController.navigate(Rutas.PANEL) {
            popUpTo(Rutas.LOGIN) { inclusive = true }
            launchSingleTop = true
        }
    }

    // La franja "MODO LOCAL / MODO SERVIDOR" va arriba en todas las pantallas; como ya ocupa la barra
    // de estado, el resto consume ese espacio para que las pantallas no lo dejen en blanco dos veces.
    Column(modifier = Modifier.fillMaxSize()) {
        FranjaModo(viewModel)
        Box(modifier = Modifier.weight(1f).fillMaxSize().consumeWindowInsets(WindowInsets.statusBars)) {
            NavHost(
                navController = navController,
                startDestination = Rutas.LOGIN,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeIn(tween(300))
                },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeOut(tween(300))
                }
            ) {
                composable(Rutas.LOGIN) {
                    LoginScreen(viewModel = viewModel, onIngresar = irAlPanel)
                }
                composable(Rutas.PANEL) {
                    PanelScreen(navController = navController, viewModel = viewModel, onCerrarSesion = cerrarSesion)
                }
                composable(Rutas.PRODUCTOS) {
                    ProductosScreen(navController = navController, viewModel = viewModel)
                }
                composable(Rutas.PRODUCTO_FORM) {
                    ProductoFormScreen(navController = navController, viewModel = viewModel)
                }
                composable(Rutas.PRODUCTO_DETALLE) {
                    ProductoDetalleScreen(navController = navController, viewModel = viewModel)
                }
                composable(Rutas.MOVIMIENTO) {
                    RegistrarMovimientoScreen(navController = navController, viewModel = viewModel)
                }
                composable(Rutas.HISTORIAL) {
                    HistorialMovimientosScreen(navController = navController, viewModel = viewModel)
                }
                composable(Rutas.REPOSICION) {
                    ReposicionScreen(navController = navController, viewModel = viewModel)
                }
            }

            SnackbarHost(
                hostState = estadoSnackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
            )
        }
    }
}
