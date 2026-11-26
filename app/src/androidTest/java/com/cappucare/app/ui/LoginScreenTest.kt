package com.cappucare.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cappucare.app.MainActivity
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * TODO(E3-12) · Pruebas de interfaz (Compose UI Test) de la pantalla de Login.
 *
 * Se ejecutan en un emulador o teléfono: clic derecho sobre esta clase > Run (o
 * `./gradlew connectedDebugAndroidTest`). Usan la app real (MainActivity) en MODO LOCAL
 * (`USAR_API=false`): así no dependen de que el servidor esté encendido.
 * Como el campo de texto no tiene `testTag`, busca por el texto de la etiqueta
 * (`onNodeWithText("Usuario (correo)")`) o agrega `Modifier.testTag("campoUsuario")` en
 * LoginScreen.kt y usa `onNodeWithTag`.
 *
 * Casos (mínimo 3; entrega al menos 1 funcionando):
 *   1. la pantalla muestra el título "Cappucare Stock" y el botón "Ingresar"
 *   2. tocar "Ingresar" con los campos vacíos muestra "Ingresa tu usuario" y "Ingresa tu clave"
 *   3. escribir encargado@cappucare.cl / clave incorrecta y tocar "Ingresar" muestra "Usuario o clave incorrectos"
 *
 * Cuando los completes, QUITA @Ignore.
 */
@Ignore("E3: completar")
class LoginScreenTest {

    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    // --- Ejemplo resuelto ---------------------------------------------------------------------------

    @Test
    fun muestraTituloYBotonIngresar() {
        regla.onNodeWithText("Cappucare Stock").assertExists()
        regla.onNodeWithText("Ingresar").assertExists()
    }

    // --- Para completar -------------------------------------------------------------------------------

    @Test
    fun conCamposVaciosMuestraLosErrores() {
        regla.onNodeWithText("Ingresar").performClick()
        fail("E3: comprobar que aparecen \"Ingresa tu usuario\" y \"Ingresa tu clave\"")
    }

    @Test
    fun conClaveIncorrectaMuestraElMensajeGenerico() {
        fail("E3: escribir usuario y clave incorrecta (performTextInput), tocar Ingresar y comprobar el mensaje")
    }
}
