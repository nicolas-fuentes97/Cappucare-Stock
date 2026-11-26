package com.cappucare.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cappucare.app.R
import com.cappucare.app.model.ErroresLogin
import com.cappucare.app.ui.components.IconoError
import com.cappucare.app.ui.components.MensajeError
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * RF-00 · Inicio de sesión del personal (primera pantalla).
 *
 * La pantalla solo dibuja y llama al ViewModel:
 *  - validarLogin() devuelve el error de cada campo (se muestra bajo el campo con ícono);
 *  - login() busca el usuario en Room (modo local) o ingresa contra la API (modo servidor, E3-7);
 *    cuando usuarioActivo deja de ser null, se navega al Panel.
 *  - El botón "Servidor" (E3-8) abre el diálogo para cambiar la dirección de la API sin recompilar
 *    y para activar el MODO LOCAL (respaldo sin servidor); "Ingresar" muestra un indicador mientras
 *    espera la respuesta.
 *
 * Usuarios de prueba: encargado@cappucare.cl / encargado123 · ventas@cappucare.cl / ventas123
 */
@Composable
fun LoginScreen(viewModel: CappucareViewModel, onIngresar: () -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var claveVisible by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf(ErroresLogin()) }
    val errorCredenciales by viewModel.errorLogin.collectAsState()
    val usuarioActivo by viewModel.usuarioActivo.collectAsState()
    val cargando by viewModel.cargandoLogin.collectAsState()
    val urlServidor by viewModel.urlServidor.collectAsState()
    val modoLocal by viewModel.modoLocal.collectAsState()
    var mostrarServidor by remember { mutableStateOf(false) }

    // RF-00 (opcional): rellena el último usuario recordado con DataStore (solo si el campo está vacío).
    val recordado by viewModel.ultimoUsuario.collectAsState()
    LaunchedEffect(recordado) {
        val guardado = recordado
        if (!guardado.isNullOrEmpty() && usuario.isEmpty()) usuario = guardado
    }

    // Navega UNA vez cuando el ViewModel confirma el usuario.
    LaunchedEffect(usuarioActivo) {
        if (usuarioActivo != null) onIngresar()
    }

    val ingresar = {
        errores = viewModel.validarLogin(usuario, clave)
        if (!errores.hayErrores && !cargando) viewModel.login(usuario.trim(), clave)
    }

    // Diálogo "Servidor": dirección de la API (E3-8) e interruptor de modo local. Siempre disponible.
    if (mostrarServidor) {
        DialogoServidor(viewModel = viewModel, onCerrar = { mostrarServidor = false })
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = "Logo",
                modifier = Modifier.size(110.dp)
            )
            Text("Cappucare Stock", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Control de existencias de insumos médicos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = usuario,
                onValueChange = {
                    usuario = it
                    errores = errores.copy(usuario = null)
                    viewModel.limpiarErrorLogin()
                },
                label = { Text("Usuario (correo)") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                trailingIcon = if (errores.usuario != null) { { IconoError() } } else null,
                isError = errores.usuario != null,
                supportingText = if (errores.usuario != null) { { MensajeError(errores.usuario.orEmpty()) } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = clave,
                onValueChange = {
                    clave = it
                    errores = errores.copy(clave = null)
                    viewModel.limpiarErrorLogin()
                },
                label = { Text("Clave") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                singleLine = true,
                isError = errores.clave != null,
                supportingText = if (errores.clave != null) { { MensajeError(errores.clave.orEmpty()) } } else null,
                // RNF-08: la clave se muestra oculta por defecto.
                visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { claveVisible = !claveVisible }) {
                        Icon(
                            imageVector = if (claveVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (claveVisible) "Ocultar clave" else "Mostrar clave"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // "Usuario o clave incorrectos" aparece con animación (no dice cuál de los dos falló).
            AnimatedVisibility(
                visible = errorCredenciales != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(errorCredenciales.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Button(modifier = Modifier.fillMaxWidth(), enabled = !cargando, onClick = ingresar) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Conectando…")
                } else {
                    Text("Ingresar")
                }
            }

            // Cambiar la dirección de la API sin recompilar (emulador, red local, Port Forwarding...)
            // y elegir entre modo servidor y modo local.
            TextButton(onClick = { mostrarServidor = true }, enabled = !cargando) {
                Icon(Icons.Filled.Dns, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (modoLocal) "Servidor: modo local (sin servidor)" else "Servidor: $urlServidor", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
