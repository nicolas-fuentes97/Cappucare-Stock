package com.cappucare.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * EA3 · Estado de la conexión con el servidor (solo en modo servidor): barra de progreso
 * mientras se sincroniza y, si falló, el mensaje en español con el botón "Reintentar". Mientras
 * hay un error se siguen mostrando los datos guardados en el equipo (la copia local en Room).
 */
@Composable
fun EstadoConexion(viewModel: CappucareViewModel, modifier: Modifier = Modifier) {
    val modoLocal by viewModel.modoLocal.collectAsState()
    val estado by viewModel.sincronizacion.collectAsState()
    val sinConexionAlIngresar by viewModel.sesionSinConexion.collectAsState()
    if (modoLocal) return   // en modo local no hay servidor: la franja superior ya lo indica

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (estado.cargando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Actualizando desde el servidor…", style = MaterialTheme.typography.labelMedium)
        }
        val mensaje = estado.error
        if (mensaje != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        mensaje,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    // Sin token (se ingresó sin conexión) no se puede sincronizar: hay que volver a ingresar.
                    if (!sinConexionAlIngresar) {
                        TextButton(onClick = { viewModel.sincronizar() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}
