package com.cappucare.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modo de operación de la app, que se puede cambiar en cualquier momento (sin reiniciar):
 *
 *  - MODO SERVIDOR: la API es la fuente de verdad y Room guarda su copia (es el modo que se
 *    demuestra en la defensa, porque integra la app con el servicio).
 *  - MODO LOCAL: no se usa la red; la app opera solo con la base Room del equipo y aplica las
 *    mismas reglas de negocio. Es un respaldo para poder demostrar la app si el servicio falla.
 *
 * Esta clase guarda el valor vigente en memoria (para leerlo en CADA operación, sin esperar a
 * DataStore) y lo expone como StateFlow (para que la interfaz y los Flow de datos reaccionen al
 * cambio). La persistencia en el equipo la hace PreferenciasModo (data/local).
 * Kotlin puro (sin Android): se prueba con JUnit.
 */
class ModoOperacion(modoLocalInicial: Boolean) {

    private val _modoLocal = MutableStateFlow(modoLocalInicial)

    /** true = modo local; false = modo servidor. */
    val modoLocal: StateFlow<Boolean> = _modoLocal.asStateFlow()

    /** Lectura inmediata del modo vigente (se consulta en cada operación). */
    val esLocal: Boolean get() = _modoLocal.value

    fun establecer(local: Boolean) {
        _modoLocal.value = local
    }

    companion object {
        /** Valor inicial de la app: lo guardado por el usuario y, si no hay nada guardado, el [inicial] de fábrica. */
        fun resolver(guardado: Boolean?, inicial: Boolean): Boolean = guardado ?: inicial
    }
}
