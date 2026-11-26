package com.cappucare.app.ui.screen.movimientos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cappucare.app.model.ErroresMovimiento
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.ui.components.BarraSuperior
import com.cappucare.app.ui.components.BotonEscanearCodigo
import com.cappucare.app.ui.components.IconoError
import com.cappucare.app.ui.components.MensajeError
import com.cappucare.app.ui.components.SelectorDesplegable
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * RF-04 / RF-05 · Registrar un ingreso (recepción de mercadería) o una salida (venta, merma, etc.).
 *
 * - viewModel.validarMovimiento() devuelve el error de cada campo (RN-02 motivo obligatorio en
 *   salidas, RN-03 no sacar más de lo disponible); la pantalla los muestra bajo cada campo.
 * - viewModel.registrarMovimiento() guarda el movimiento y la nueva cantidad en una transacción.
 * - RN-05 / RF-06: la fecha y el responsable NO se piden: los asigna el ViewModel.
 * - RNF-06: el producto también se puede elegir escaneando su código con la cámara.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrarMovimientoScreen(navController: NavHostController, viewModel: CappucareViewModel) {
    val productos by viewModel.productos.collectAsState()
    val seleccionadoId by viewModel.productoSeleccionadoId.collectAsState()
    val usuario by viewModel.usuarioActivo.collectAsState()

    var productoId by remember { mutableStateOf(seleccionadoId) }
    val producto: Producto? = productos.find { it.id == productoId }   // siempre con la cantidad actual
    var tipo by remember { mutableStateOf(TipoMovimiento.INGRESO) }
    var cantidad by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf<MotivoSalida?>(null) }
    var observacion by remember { mutableStateOf("") }
    var errores by remember { mutableStateOf(ErroresMovimiento()) }

    Scaffold(topBar = { BarraSuperior("Registrar movimiento", navController) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectorDesplegable<Producto>(
                    etiqueta = "Producto",
                    opciones = productos,
                    seleccion = producto,
                    textoDe = { "${it.codigo} · ${it.nombre}" },
                    onSeleccion = {
                        productoId = it.id
                        errores = errores.copy(producto = null)
                    },
                    error = errores.producto,
                    modifier = Modifier.weight(1f)
                )
                // RNF-06: elegir el producto escaneando su código.
                BotonEscanearCodigo(
                    onCodigoLeido = { codigo ->
                        val encontrado = viewModel.productoPorCodigo(codigo)
                        if (encontrado != null) {
                            productoId = encontrado.id
                            errores = errores.copy(producto = null)
                        } else {
                            viewModel.avisar("No hay un producto con el código $codigo")
                        }
                    },
                    onPermisoDenegado = {
                        viewModel.avisar("Sin permiso de cámara: elige el producto de la lista")
                    }
                )
            }
            if (producto != null) {
                Text(
                    "Disponible actualmente: ${producto.cantidadDisponible} ${producto.unidadMedida.etiqueta.lowercase()} · Estado: ${producto.estado.etiqueta}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text("Tipo de movimiento", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TipoMovimiento.entries.forEachIndexed { indice, opcion ->
                    SegmentedButton(
                        selected = tipo == opcion,
                        onClick = {
                            tipo = opcion
                            errores = errores.copy(motivo = null, cantidad = null)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = indice, count = TipoMovimiento.entries.size),
                        icon = {
                            Icon(
                                if (opcion == TipoMovimiento.INGRESO) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                contentDescription = null
                            )
                        }
                    ) {
                        Text(if (opcion == TipoMovimiento.INGRESO) "Ingreso" else "Salida")
                    }
                }
            }

            OutlinedTextField(
                value = cantidad,
                onValueChange = {
                    cantidad = it
                    errores = errores.copy(cantidad = null)
                },
                label = { Text("Cantidad") },
                leadingIcon = { Icon(Icons.Filled.Numbers, contentDescription = null) },
                trailingIcon = if (errores.cantidad != null) { { IconoError() } } else null,
                singleLine = true,
                isError = errores.cantidad != null,
                supportingText = if (errores.cantidad != null) {
                    { MensajeError(errores.cantidad.orEmpty()) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // El motivo solo aparece en las salidas (RN-02), con animación. Se elige con un toque
            // (chips visibles) para que registrar una venta desde el detalle sea rápido (RNF-02).
            AnimatedVisibility(
                visible = tipo == TipoMovimiento.SALIDA,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Text("Motivo de la salida", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MotivoSalida.entries.forEach { opcion ->
                            FilterChip(
                                selected = motivo == opcion,
                                onClick = {
                                    motivo = opcion
                                    errores = errores.copy(motivo = null)
                                },
                                label = { Text(opcion.etiqueta) }
                            )
                        }
                    }
                    val errorMotivo = errores.motivo
                    if (errorMotivo != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) { MensajeError(errorMotivo) }
                    }
                }
            }

            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación (opcional)") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Responsable: ${usuario?.nombre.orEmpty()} · La fecha y hora se registran automáticamente.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onClick = {
                    val elegido = producto
                    errores = viewModel.validarMovimiento(elegido?.id, tipo, cantidad, motivo)
                    if (!errores.hayErrores && elegido != null) {
                        viewModel.registrarMovimiento(
                            productoId = elegido.id,
                            tipo = tipo,
                            cantidad = cantidad.trim().toInt(),
                            motivo = if (tipo == TipoMovimiento.SALIDA) motivo else null,
                            observacion = observacion.trim()
                        )
                        navController.popBackStack()
                    }
                }
            ) { Text(if (tipo == TipoMovimiento.INGRESO) "Registrar ingreso" else "Registrar salida") }
        }
    }
}
