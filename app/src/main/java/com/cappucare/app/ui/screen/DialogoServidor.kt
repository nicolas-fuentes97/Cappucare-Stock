package com.cappucare.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cappucare.app.data.remote.ResultadoPruebaServidor
import com.cappucare.app.ui.components.IconoError
import com.cappucare.app.ui.components.MensajeError
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * Diálogo "Servidor" (se abre desde el Login): permite cambiar la dirección de la API sin
 * recompilar, por ejemplo a la URL https de Port Forwarding el día de la presentación, y elegir
 * el modo de operación:
 *  - Interruptor "Modo local (sin servidor)": la app usa solo la base del equipo (respaldo para
 *    demostrar si no hay servicio). Se aplica al instante y queda guardado. Si "Probar conexión"
 *    falla, el diálogo ofrece "Usar modo local".
 *  - Valida la dirección mientras se escribe (isError + supportingText).
 *  - "Probar conexión" llama a /health y muestra si responde.
 *  - "Guardar" la deja en DataStore; "Restablecer" vuelve a la dirección de fábrica.
 */
@Composable
fun DialogoServidor(viewModel: CappucareViewModel, onCerrar: () -> Unit) {
    val actual by viewModel.urlServidor.collectAsState()
    val modoLocal by viewModel.modoLocal.collectAsState()
    var texto by remember { mutableStateOf(actual) }
    var error by remember { mutableStateOf<String?>(null) }
    var probando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<ResultadoPruebaServidor?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Filled.Dns, contentDescription = null) },
        title = { Text("Servidor") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo local (sin servidor)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Usa solo la base de datos de este equipo, con las mismas reglas de negocio. " +
                                "Es un respaldo para demostrar la app sin servidor: lo que crees en modo local " +
                                "no se envía al servidor ni se sincroniza al volver al modo servidor.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = modoLocal,
                        onCheckedChange = { viewModel.cambiarModoLocal(it) },
                        modifier = Modifier.testTag("interruptorModoLocal")
                    )
                }
                if (modoLocal) {
                    TextButton(onClick = { viewModel.restablecerDatosLocales() }) { Text("Restablecer datos locales") }
                }

                Text(
                    "Dirección de la API. Emulador: http://10.0.2.2:3000/ · Teléfono en la misma red: " +
                        "http://IP-del-PC:3000/ · Port Forwarding o Internet: https://…",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = {
                        texto = it
                        error = viewModel.validarUrlServidor(it).takeIf { _ -> it.isNotBlank() }
                        resultado = null
                    },
                    label = { Text("Dirección del servidor") },
                    singleLine = true,
                    isError = error != null,
                    trailingIcon = if (error != null) { { IconoError() } } else null,
                    supportingText = if (error != null) { { MensajeError(error.orEmpty()) } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        enabled = !probando,
                        onClick = {
                            val problema = viewModel.validarUrlServidor(texto)
                            if (problema != null) {
                                error = problema
                            } else {
                                probando = true
                                resultado = null
                                viewModel.probarServidor(texto) { r ->
                                    probando = false
                                    resultado = r
                                }
                            }
                        }
                    ) { Text("Probar conexión") }
                    if (probando) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }

                resultado?.let { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            if (r.exito) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = if (r.exito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            r.mensaje,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (r.exito) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (!modoLocal && resultado?.exito == false) {
                    TextButton(onClick = { viewModel.cambiarModoLocal(true) }) { Text("Usar modo local") }
                }

                TextButton(onClick = {
                    viewModel.restablecerUrlServidor {
                        texto = viewModel.urlServidor.value
                        error = null
                        resultado = null
                    }
                }) { Text("Restablecer dirección de fábrica") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.guardarUrlServidor(texto) { problema ->
                    if (problema == null) onCerrar() else error = problema
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
