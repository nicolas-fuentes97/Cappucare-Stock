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

private val Context.almacenServidor by preferencesDataStore(name = "servidor")

/**
 * Dirección del servidor elegida por el usuario en Login > Servidor (DataStore). Si no hay una
 * guardada se usa la de BuildConfig.API_BASE_URL. Solo se guarda la dirección, nunca claves.
 */
class PreferenciasServidor(private val context: Context) {

    private val claveUrl = stringPreferencesKey("url_servidor")

    /** (Ya resuelta.) null = el usuario no eligió una dirección propia. */
    val urlGuardada: Flow<String?> = context.almacenServidor.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias -> preferencias[claveUrl] }

    /**
     * TODO(E3-8c) · Guarda [url] (ya normalizada) en DataStore con `context.almacenServidor.edit`
     * y la clave [claveUrl].
     * CRITERIO DE ACEPTACIÓN: tras guardar, cerrar la app y volver a abrirla, el diálogo Servidor
     * muestra esa dirección.
     */
    suspend fun guardar(url: String) {
        TODO("E3-8c: context.almacenServidor.edit { it[claveUrl] = url }")
    }

    /** TODO(E3-8c) · Borra la dirección guardada (vuelve a la de BuildConfig.API_BASE_URL). */
    suspend fun restablecer() {
        TODO("E3-8c: context.almacenServidor.edit { it.remove(claveUrl) }")
    }
}
