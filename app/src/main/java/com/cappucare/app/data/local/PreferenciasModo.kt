package com.cappucare.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.almacenModo by preferencesDataStore(name = "modo")

/**
 * Preferencia persistente "modo local" (DataStore): true = la app opera solo con la base Room del
 * equipo (sin servidor); false = usa el servidor. Se cambia en Login > Servidor y sobrevive al
 * cierre de la app. Mientras el usuario no elija nada, no hay valor guardado y rige el valor de
 * fábrica (ver CappucareViewModel).
 */
class PreferenciasModo(private val context: Context) {

    private val claveModoLocal = booleanPreferencesKey("modo_local")

    /** null = el usuario nunca eligió un modo. */
    val modoLocalGuardado: Flow<Boolean?> = context.almacenModo.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias -> preferencias[claveModoLocal] }

    suspend fun guardar(modoLocal: Boolean) {
        context.almacenModo.edit { preferencias -> preferencias[claveModoLocal] = modoLocal }
    }
}
