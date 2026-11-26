package com.cappucare.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.almacenSesion by preferencesDataStore(name = "sesion")

/**
 * RF-00 (opcional) · Recuerda con DataStore el último usuario (correo) que inició sesión, para
 * rellenar el campo en el próximo ingreso. NUNCA se guarda la clave.
 */
class PreferenciasSesion(private val context: Context) {

    private val claveUltimoUsuario = stringPreferencesKey("ultimo_usuario")

    val ultimoUsuario: Flow<String> = context.almacenSesion.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias -> preferencias[claveUltimoUsuario].orEmpty() }

    suspend fun guardarUltimoUsuario(usuario: String) {
        context.almacenSesion.edit { preferencias -> preferencias[claveUltimoUsuario] = usuario }
    }
}
