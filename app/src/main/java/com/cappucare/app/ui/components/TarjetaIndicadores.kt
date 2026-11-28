package com.cappucare.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cappucare.app.model.Indicador
import com.cappucare.app.reglas.FormatoIndicadores
import com.cappucare.app.viewmodel.IndicadoresUiState
import com.cappucare.app.viewmodel.IndicadoresViewModel

/**
 * API EXTERNA en la interfaz: tarjeta del Panel con los indicadores del día (mindicador.cl).
 * Usa su propio ViewModel, así un error de la API no afecta al inventario (Room).
 * Los tres estados de la Guía 14: Cargando (barra), Éxito (valores) y Error (mensaje + Reintentar).
 */
@Composable
fun TarjetaIndicadores(
    modifier: Modifier = Modifier,
    viewModel: IndicadoresViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    Card(modifier = modifier.fillMaxWidth().testTag("tarjetaIndicadores")) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Indicadores del día", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Para cotizar compras · fuente: mindicador.cl",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.cargarIndicadores() },
                    enabled = estado !is IndicadoresUiState.Cargando
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar indicadores")
                }
            }

            when (val e = estado) {
                is IndicadoresUiState.Cargando ->
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                is IndicadoresUiState.Exito -> {
                    // Dos filas de dos valores: se lee bien en teléfono y en tablet.
                    e.indicadores.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            fila.forEach { indicador -> ValorIndicador(indicador, Modifier.weight(1f)) }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    e.indicadores.firstOrNull()?.let {
                        Text(
                            "Valores al ${FormatoIndicadores.fecha(it.fecha)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is IndicadoresUiState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(e.mensaje, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    OutlinedButton(onClick = { viewModel.cargarIndicadores() }) { Text("Reintentar") }
                }
            }
        }
    }
}

@Composable
private fun ValorIndicador(indicador: Indicador, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(FormatoIndicadores.nombreCorto(indicador), style = MaterialTheme.typography.labelMedium)
            Text(FormatoIndicadores.valor(indicador), style = MaterialTheme.typography.titleMedium)
        }
    }
}
