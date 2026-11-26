package com.cappucare.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * Franja visible en TODAS las pantallas que indica con qué fuente de datos opera la app:
 * "MODO LOCAL" (solo la base del equipo, respaldo) o "MODO SERVIDOR" (la API). Sirve para que,
 * en una demostración, se note qué se está mostrando. Se dibuja sobre la barra de estado
 * (consume ese espacio para que las pantallas no lo repitan).
 */
@Composable
fun FranjaModo(viewModel: CappucareViewModel, modifier: Modifier = Modifier) {
    val local by viewModel.modoLocal.collectAsState()
    val urlServidor by viewModel.urlServidor.collectAsState()

    val contenedor = if (local) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
    val contenido = if (local) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        color = contenedor,
        contentColor = contenido,
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (local) "franjaModoLocal" else "franjaModoServidor")
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (local) "MODO LOCAL" else "MODO SERVIDOR",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                if (local) "datos solo de este equipo (respaldo, sin servidor)" else urlServidor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
