package com.cappucare.app.modo

import com.cappucare.app.data.ModoOperacion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Modo local / modo servidor: valor inicial, preferencia guardada y cambio en caliente (sin Android). */
class ModoOperacionTest {

    @Test
    fun sinPreferenciaGuardadaRigeElValorDeFabrica() {
        assertFalse(ModoOperacion.resolver(guardado = null, inicial = false))
        assertTrue(ModoOperacion.resolver(guardado = null, inicial = true))
    }

    @Test
    fun laPreferenciaGuardadaMandaSobreElValorDeFabrica() {
        assertTrue(ModoOperacion.resolver(guardado = true, inicial = false))
        assertFalse(ModoOperacion.resolver(guardado = false, inicial = true))
    }

    @Test
    fun elValorInicialSeLeeDeInmediato() {
        assertFalse(ModoOperacion(modoLocalInicial = false).esLocal)
        assertTrue(ModoOperacion(modoLocalInicial = true).esLocal)
    }

    @Test
    fun establecerCambiaElModoDeInmediatoYLoPublicaEnElFlujo() = runBlocking {
        val modo = ModoOperacion(modoLocalInicial = false)
        modo.establecer(true)
        assertTrue(modo.esLocal)
        assertTrue(modo.modoLocal.first())
        modo.establecer(false)
        assertEquals(false, modo.modoLocal.value)
    }
}
