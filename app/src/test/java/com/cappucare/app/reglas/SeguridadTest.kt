package com.cappucare.app.reglas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de RNF-08: las claves se guardan como resumen SHA-256 con sal, nunca en texto plano. */
class SeguridadTest {

    @Test
    fun `el resumen tiene 64 caracteres hexadecimales`() {
        val hash = Seguridad.hash("encargado123")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `la misma clave da siempre el mismo resumen`() {
        assertEquals(Seguridad.hash("ventas123"), Seguridad.hash("ventas123"))
    }

    @Test
    fun `claves distintas dan resumenes distintos`() {
        assertNotEquals(Seguridad.hash("ventas123"), Seguridad.hash("ventas124"))
    }

    @Test
    fun `el resumen no contiene la clave en texto plano`() {
        assertFalse(Seguridad.hash("encargado123").contains("encargado123"))
    }

    @Test
    fun `verificar acepta la clave correcta y rechaza la incorrecta`() {
        val guardado = Seguridad.hash("encargado123")
        assertTrue(Seguridad.verificar("encargado123", guardado))
        assertFalse(Seguridad.verificar("Encargado123", guardado))
        assertFalse(Seguridad.verificar("", guardado))
    }

    @Test
    fun `usa la sal del proyecto (no es el SHA-256 de la clave sola)`() {
        // SHA-256("abc") es ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertNotEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Seguridad.hash("abc"))
    }
}
