package com.cappucare.app.ui.screen.productos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.navigation.Rutas
import com.cappucare.app.ui.components.BarraSuperior
import com.cappucare.app.ui.components.SelectorDesplegable
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * RF-08 · Detalle de un producto: todos sus datos y su disponibilidad actual.
 * RF-03 · Cambio de estado (con confirmación antes de "Dado de baja").
 * Accesos a registrar un movimiento (RF-04/05) o ver su historial (RF-10).
 * RN-08: "Actualizar estado" y "Editar datos" solo los ve el Personal encargado.
 */
@Composable
fun ProductoDetalleScreen(navController: NavHostController, viewModel: CappucareViewModel) {
    val seleccionadoId by viewModel.productoSeleccionadoId.collectAsState()
    val productos by viewModel.productos.collectAsState()
    val producto = productos.find { it.id == seleccionadoId }
    val usuario by viewModel.usuarioActivo.collectAsState()

    Scaffold(topBar = { BarraSuperior("Detalle del producto", navController) }) { padding ->
        if (producto == null) {
            Text("Producto no encontrado.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        var nuevoEstado by remember(producto.id) { mutableStateOf(producto.estado) }
        var confirmarBaja by remember { mutableStateOf(false) }
        val puedeEditar = usuario?.perfil?.puedeEditarCatalogo == true   // RN-08

        // Confirmación antes de una acción destructiva (dar de baja un producto).
        if (confirmarBaja) {
            AlertDialog(
                onDismissRequest = { confirmarBaja = false },
                icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                title = { Text("¿Dar de baja este producto?") },
                text = {
                    Text(
                        "\"${producto.nombre}\" dejará de aparecer en las alertas de reposición y no " +
                            "se podrá vender. Su historial se conserva."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmarBaja = false
                        viewModel.cambiarEstado(producto.id, EstadoProducto.DADO_DE_BAJA)
                    }) { Text("Dar de baja") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmarBaja = false }) { Text("Cancelar") }
                }
            )
        }

        // RF-09: aviso de reposición (misma regla RN-07 del ViewModel). Aparece con animación.
        val bajoMinimo = viewModel.estaBajoMinimo(producto)
        val visibilidadAviso = remember(producto.id) { MutableTransitionState(false) }
        LaunchedEffect(bajoMinimo) { visibilidadAviso.targetState = bajoMinimo }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visibleState = visibilidadAviso,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Bajo su nivel mínimo", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Quedan ${producto.cantidadDisponible} de ${producto.nivelMinimo}. " +
                                    "Faltan ${viewModel.faltaParaMinimo(producto)}: registra un ingreso para reponer.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(producto.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(producto.descripcion, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Dato("Código", producto.codigo)
                    Dato("Categoría", producto.categoria.etiqueta)
                    Dato("Unidad de medida", producto.unidadMedida.etiqueta)
                    Dato("Cantidad disponible", producto.cantidadDisponible.toString())
                    Dato("Nivel mínimo", producto.nivelMinimo.toString())
                    Dato("Ubicación", producto.ubicacion)
                    Dato("Estado", producto.estado.etiqueta)
                }
            }

            if (puedeEditar) {
                Text("Actualizar estado", style = MaterialTheme.typography.titleSmall)
                SelectorDesplegable(
                    etiqueta = "Nuevo estado",
                    opciones = EstadoProducto.entries,
                    seleccion = nuevoEstado,
                    textoDe = { it.etiqueta },
                    onSeleccion = { nuevoEstado = it }
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (nuevoEstado == EstadoProducto.DADO_DE_BAJA && producto.estado != EstadoProducto.DADO_DE_BAJA) {
                            confirmarBaja = true
                        } else {
                            viewModel.cambiarEstado(producto.id, nuevoEstado)
                        }
                    }
                ) { Text("Guardar estado") }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.MOVIMIENTO) }) {
                Text("Registrar ingreso o salida")
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.HISTORIAL) }) {
                Text("Ver historial de este producto")
            }
            if (puedeEditar) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.PRODUCTO_FORM) }) {
                    Text("Editar datos")
                }
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Text("$etiqueta: $valor", style = MaterialTheme.typography.bodyMedium)
}
