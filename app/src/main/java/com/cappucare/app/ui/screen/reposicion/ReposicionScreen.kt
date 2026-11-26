package com.cappucare.app.ui.screen.reposicion

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cappucare.app.model.Producto
import com.cappucare.app.navigation.Rutas
import com.cappucare.app.notificacion.NotificadorStock
import com.cappucare.app.ui.components.BarraSuperior
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * RF-09 · Módulo "Consulta y reposición": productos que necesitan nueva compra (cantidad
 * disponible bajo su nivel mínimo), del más urgente al menos urgente.
 *
 * La pantalla solo DIBUJA: qué productos aparecen, cuánto falta y la cobertura los calcula el
 * ViewModel (ReglaReposicion). Animaciones: barra de cobertura que se llena al entrar, contador
 * animado, entrada/salida de tarjetas y aviso de permisos que se oculta al concederlo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReposicionScreen(navController: NavHostController, viewModel: CappucareViewModel) {
    // Ambas listas vienen de Room como StateFlow: la pantalla se redibuja sola tras un movimiento.
    val productos by viewModel.productos.collectAsState()
    val bajoMinimo by viewModel.alertasReposicion.collectAsState()   // RF-09 (regla ReglaReposicion sobre el catálogo)

    Scaffold(topBar = { BarraSuperior("Alertas de reposición", navController) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "resumen") {
                ResumenReposicion(cantidad = bajoMinimo.size, revisados = productos.size)
            }
            item(key = "permiso") {
                AvisoPermisoNotificaciones()
            }
            items(bajoMinimo, key = { it.id }) { producto ->
                TarjetaAlerta(
                    producto = producto,
                    falta = viewModel.faltaParaMinimo(producto),
                    cobertura = viewModel.nivelDeCobertura(producto),
                    onVerDetalle = {
                        viewModel.seleccionarProducto(producto.id)
                        navController.navigate(Rutas.PRODUCTO_DETALLE)
                    },
                    onRegistrarIngreso = {
                        // RegistrarMovimientoScreen ya preselecciona el producto y el tipo "Ingreso".
                        viewModel.seleccionarProducto(producto.id)
                        navController.navigate(Rutas.MOVIMIENTO)
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

/** Cabecera: cuántos productos necesitan reposición (o mensaje positivo si ninguno). */
@Composable
private fun ResumenReposicion(cantidad: Int, revisados: Int) {
    val hayAlertas = cantidad > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hayAlertas) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (hayAlertas) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                // El número cambia con una animación (sube/baja) cuando cambia la cantidad.
                AnimatedContent(
                    targetState = cantidad,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "contadorReposicion"
                ) { n ->
                    Text(
                        text = when (n) {
                            0 -> "Todo el stock está sobre su nivel mínimo"
                            1 -> "1 producto necesita reposición"
                            else -> "$n productos necesitan reposición"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = if (hayAlertas) "Ordenados de mayor a menor falta. Toca uno para ver su detalle."
                    else "Productos revisados: $revisados.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * RNF-07 (opcional): en Android 13 o superior las notificaciones necesitan permiso en tiempo
 * de ejecución. Se pide SOLO cuando el usuario toca "Activar" (no al abrir la app). Si lo
 * rechaza, la app sigue funcionando: la lista de alertas no depende del permiso.
 */
@Composable
private fun AvisoPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return // antes de Android 13 no se pide

    val context = LocalContext.current
    var concedido by remember { mutableStateOf(NotificadorStock.tienePermiso(context)) }
    var rechazado by remember { mutableStateOf(false) }
    val lanzadorPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        concedido = ok
        rechazado = !ok
    }

    AnimatedVisibility(visible = !concedido, exit = fadeOut() + shrinkVertically()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avisos de stock mínimo", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (rechazado)
                            "Sin permiso no se envían avisos, pero esta lista sigue funcionando. Puedes activarlo en Ajustes."
                        else
                            "Activa las notificaciones para recibir un aviso cuando un producto quede bajo su mínimo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Activar")
                }
            }
        }
    }
}

/** Tarjeta de un producto bajo su mínimo, con barra de cobertura animada. */
@Composable
private fun TarjetaAlerta(
    producto: Producto,
    falta: Int,
    cobertura: Float,
    onVerDetalle: () -> Unit,
    onRegistrarIngreso: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sinStock = producto.cantidadDisponible == 0
    val colorAlerta = if (sinStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    // Animación: la barra parte vacía y se llena hasta la cobertura real del producto.
    val progreso = remember(producto.id) { Animatable(0f) }
    LaunchedEffect(cobertura) { progreso.animateTo(cobertura, tween(durationMillis = 700)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onVerDetalle)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (sinStock) Icons.Filled.ErrorOutline else Icons.Filled.Warning,
                    contentDescription = if (sinStock) "Sin stock" else "Bajo el mínimo",
                    tint = colorAlerta
                )
                Spacer(Modifier.width(8.dp))
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (sinStock) {
                    Surface(color = MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.small) {
                        Text(
                            "Sin stock",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text("${producto.codigo} · ${producto.ubicacion}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Disponible: ${producto.cantidadDisponible} de ${producto.nivelMinimo} (mínimo)",
                color = colorAlerta,
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { progreso.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = colorAlerta,
                trackColor = colorAlerta.copy(alpha = 0.2f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Faltan $falta para llegar al mínimo",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRegistrarIngreso) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Registrar ingreso")
                }
            }
        }
    }
}
