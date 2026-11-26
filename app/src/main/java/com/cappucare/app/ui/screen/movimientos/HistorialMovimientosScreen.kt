package com.cappucare.app.ui.screen.movimientos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.ui.components.BarraSuperior
import com.cappucare.app.viewmodel.CappucareViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val VerdeIngreso = Color(0xFF2E7D32)

/**
 * RF-10 · Historial comprensible de ingresos y salidas, del más reciente al más antiguo.
 * Desde el detalle de un producto muestra solo ese producto; desde el Panel, todos.
 *
 *  - Cada movimiento muestra cantidad con signo, motivo, observación, fecha, responsable con su
 *    perfil (RF-06) y el saldo que dejó en el producto.
 *  - Resumen de unidades ingresadas y retiradas, y filtro opcional por tipo.
 *  - Solo lectura (RN-06): no hay botones para editar ni eliminar movimientos.
 *  - Recurso nativo: "Exportar CSV" guarda el historial visible con el selector de archivos del
 *    sistema (Storage Access Framework); no requiere permisos de almacenamiento.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistorialMovimientosScreen(navController: NavHostController, viewModel: CappucareViewModel) {
    val seleccionadoId by viewModel.productoSeleccionadoId.collectAsState()
    val movimientos by viewModel.movimientos.collectAsState()
    val productos by viewModel.productos.collectAsState()
    var filtroTipo by remember { mutableStateOf<TipoMovimiento?>(null) }

    val producto = productos.find { it.id == seleccionadoId }
    val historial = remember(movimientos, productos, seleccionadoId, filtroTipo) {
        viewModel.historialDe(seleccionadoId, filtroTipo)
    }
    val resumen = remember(movimientos, seleccionadoId) { viewModel.resumenHistorial(seleccionadoId) }

    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val lanzadorExportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { destino ->
        if (destino != null) {
            val contenido = viewModel.csvHistorial(historial).toByteArray(Charsets.UTF_8)
            alcance.launch {
                val guardado = withContext(Dispatchers.IO) {
                    runCatching {
                        val salida = contexto.contentResolver.openOutputStream(destino)
                            ?: error("No se pudo abrir el archivo")
                        salida.use { it.write(BOM_UTF8 + contenido) }
                    }.isSuccess
                }
                viewModel.avisar(if (guardado) "Historial exportado" else "No se pudo guardar el archivo")
            }
        }
    }

    val titulo = if (producto != null) "Historial: ${producto.nombre}" else "Historial de movimientos"

    Scaffold(topBar = { BarraSuperior(titulo, navController) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ingresado: +${resumen.totalIngresos}", color = VerdeIngreso, style = MaterialTheme.typography.titleSmall)
                        Text("Retirado: −${resumen.totalSalidas}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                    }
                    if (producto != null) {
                        Text("Disponible ahora: ${producto.cantidadDisponible}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                FilterChip(selected = filtroTipo == null, onClick = { filtroTipo = null }, label = { Text("Todos") })
                FilterChip(
                    selected = filtroTipo == TipoMovimiento.INGRESO,
                    onClick = { filtroTipo = TipoMovimiento.INGRESO },
                    label = { Text("Ingresos") }
                )
                FilterChip(
                    selected = filtroTipo == TipoMovimiento.SALIDA,
                    onClick = { filtroTipo = TipoMovimiento.SALIDA },
                    label = { Text("Salidas") }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${historial.size} movimiento(s), del más reciente al más antiguo",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    enabled = historial.isNotEmpty(),
                    onClick = { lanzadorExportar.launch(NOMBRE_ARCHIVO) }
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Exportar CSV")
                }
            }

            if (historial.isEmpty()) {
                Text(
                    if (producto != null && filtroTipo == null) "Este producto no tiene movimientos"
                    else "No hay movimientos para mostrar",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historial, key = { it.movimiento.id }) { item ->
                    val movimiento = item.movimiento
                    val nombreProducto = productos.find { it.id == movimiento.productoId }?.nombre ?: movimiento.productoId
                    val esIngreso = movimiento.tipo == TipoMovimiento.INGRESO
                    val color = if (esIngreso) VerdeIngreso else MaterialTheme.colorScheme.error
                    Card(modifier = Modifier.animateItem().fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (esIngreso) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                contentDescription = if (esIngreso) "Ingreso" else "Salida",
                                tint = color
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "${if (esIngreso) "+" else "−"}${movimiento.cantidad} · $nombreProducto",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = color
                                )
                                Text(
                                    movimiento.tipo.etiqueta + (movimiento.motivo?.let { " · Motivo: ${it.etiqueta}" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text("Saldo después del movimiento: ${item.saldo}", style = MaterialTheme.typography.bodySmall)
                                if (movimiento.observacion.isNotBlank()) {
                                    Text(movimiento.observacion, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "${movimiento.fecha} · ${movimiento.responsable} (${movimiento.responsablePerfil.etiqueta})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val NOMBRE_ARCHIVO = "historial_cappucare.csv"

/** Marca de orden de bytes UTF-8: hace que Excel muestre bien las tildes del archivo. */
private val BOM_UTF8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
