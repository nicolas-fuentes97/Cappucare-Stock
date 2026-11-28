package com.cappucare.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.cappucare.app.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de interfaz (Compose UI Test) de la pantalla de Login, en un emulador o en la tablet:
 * clic derecho sobre esta clase > Run (o `./gradlew connectedDebugAndroidTest`).
 * Usan la app real (MainActivity) con su base Room. Como los campos no tienen testTag, se buscan
 * por el texto de su etiqueta ("Usuario (correo)", "Clave").
 */
@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {

    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    @Test
    fun muestraTituloYBotonIngresar() {
        regla.onNodeWithText("Cappucare Stock").assertExists()
        regla.onNodeWithText("Ingresar").assertExists()
    }

    @Test
    fun conCamposVaciosMuestraLosErrores() {
        // El usuario puede venir recordado (DataStore): se borra para probar los dos campos vacíos.
        regla.onNodeWithText("Usuario (correo)").performTextClearance()
        regla.onNodeWithText("Ingresar").performClick()
        regla.onNodeWithText("Ingresa tu usuario").assertExists()
        regla.onNodeWithText("Ingresa tu clave").assertExists()
    }

    @Test
    fun conClaveIncorrectaMuestraElMensajeGenerico() {
        regla.onNodeWithText("Usuario (correo)").performTextClearance()
        regla.onNodeWithText("Usuario (correo)").performTextInput("encargado@cappucare.cl")
        regla.onNodeWithText("Clave").performTextInput("clave-incorrecta")
        regla.onNodeWithText("Ingresar").performClick()
        // El login consulta Room en segundo plano: se espera a que aparezca el mensaje.
        regla.waitUntilAtLeastOneExists(hasText("Usuario o clave incorrectos"), timeoutMillis = 5_000)
    }
}
