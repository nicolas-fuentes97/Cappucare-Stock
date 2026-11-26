package com.cappucare.app.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cappucare.app.navigation.Rutas
import com.cappucare.app.ui.components.BarraSuperior
import com.cappucare.app.ui.components.EstadoConexion
import com.cappucare.app.viewmodel.CappucareViewModel
import androidx.navigation.NavHostController

/**
 * Panel principal después del login: resumen del inventario y acceso a cada módulo.
 *
 * Los contadores se actualizan solos: leen StateFlow del ViewModel que vienen de Room.
 * RN-08: las acciones de catálogo (crear/editar/estado) se ocultan al Usuario autorizado en
 * Productos y en el Detalle; aquí ambos perfiles ven lo mismo.
 */
@Composable
fun PanelScreen(
    navController: NavHostController,
    viewModel: CappucareViewModel,
    onCerrarSesion: () -> Unit
) {
    val usuario by viewModel.usuarioActivo.collectAsState()
    val productos by viewModel.productos.collectAsState()
    val movimientos by viewModel.movimientos.collectAsState()
    val bajoMinimo by viewModel.alertasReposicion.collectAsState()   // RF-09 (regla ReglaReposicion sobre el catálogo)

    Scaffold(topBar = { BarraSuperior("Cappucare Stock", navController, onCerrarSesion) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Hola, ${usuario?.nombre.orEmpty()}", style = MaterialTheme.typography.titleLarge)
            Text("Perfil: ${usuario?.perfil?.etiqueta.orEmpty()}", style = MaterialTheme.typography.bodyMedium)

            EstadoConexion(viewModel)       // EA3: progreso y errores de la API (no se dibuja en modo local)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TarjetaResumen("Productos", productos.size.toString(), Modifier.weight(1f))
                // RF-09: contador de productos bajo su mínimo; tocarlo abre Alertas de reposición.
                TarjetaBajoMinimo(
                    cantidad = bajoMinimo.size,
                    onClick = { navController.navigate(Rutas.REPOSICION) },
                    modifier = Modifier.weight(1f)
                )
                TarjetaResumen("Movimientos", movimientos.size.toString(), Modifier.weight(1f))
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.PRODUCTOS) }) {
                Text("Productos e insumos")
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                viewModel.seleccionarProducto(null)
                navController.navigate(Rutas.MOVIMIENTO)
            }) { Text("Registrar ingreso o salida") }
            Button(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.REPOSICION) }) {
                Text("Alertas de reposición")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                viewModel.seleccionarProducto(null)
                navController.navigate(Rutas.HISTORIAL)
            }) { Text("Historial de movimientos") }
        }
    }
}

@Composable
private fun TarjetaResumen(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineSmall)
            Text(titulo, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * RF-09: igual que TarjetaResumen, pero se pinta en color de alerta cuando hay productos bajo
 * su mínimo y el ícono "late" (animación infinita) para llamar la atención del encargado.
 */
@Composable
private fun TarjetaBajoMinimo(cantidad: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hayAlertas = cantidad > 0
    val colorFondo by animateColorAsState(
        targetValue = if (hayAlertas) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "fondoBajoMinimo"
    )
    val pulso = rememberInfiniteTransition(label = "pulsoAlerta")
    val alfaIcono by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "alfaIcono"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colorFondo)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cantidad.toString(), style = MaterialTheme.typography.headlineSmall)
                if (hayAlertas) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Hay productos bajo su mínimo",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(alfaIcono)
                    )
                }
            }
            Text("Bajo mínimo", style = MaterialTheme.typography.labelMedium)
        }
    }
}
