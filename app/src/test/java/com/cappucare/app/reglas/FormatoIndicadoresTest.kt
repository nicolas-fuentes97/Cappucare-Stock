package com.cappucare.app.reglas

import com.cappucare.app.model.Indicador
import org.junit.Assert.assertEquals
import org.junit.Test

/** Formato chileno de los indicadores de la API externa. */
class FormatoIndicadoresTest {

    private fun indicador(codigo: String, valor: Double, unidad: String = "Pesos") =
        Indicador(codigo, "Nombre", valor, unidad, "2026-11-25")

    @Test
    fun losPesosUsanPuntoDeMilesYComaDecimal() {
        assertEquals("$ 39.700,50", FormatoIndicadores.valor(indicador("uf", 39700.5)))
        assertEquals("$ 950,12", FormatoIndicadores.valor(indicador("dolar", 950.12)))
    }

    @Test
    fun porcentajesYDolaresTienenSuPropioSimbolo() {
        assertEquals("0,40 %", FormatoIndicadores.valor(indicador("ipc", 0.4, "Porcentaje")))
        assertEquals("US$ 4,12", FormatoIndicadores.valor(indicador("libra_cobre", 4.12, "Dólar")))
    }

    @Test
    fun laFechaSeMuestraComoDiaMesAnio() {
        assertEquals("25-11-2026", FormatoIndicadores.fecha("2026-11-25"))
        assertEquals("25-11-2026", FormatoIndicadores.fecha("2026-11-25T03:00:00.000Z"))
        assertEquals("sin fecha", FormatoIndicadores.fecha("sin fecha"))
        assertEquals("", FormatoIndicadores.fecha(""))
    }

    @Test
    fun nombresCortosParaLaTarjetaDelPanel() {
        assertEquals("Dólar", FormatoIndicadores.nombreCorto(indicador("dolar", 1.0)))
        assertEquals("Euro", FormatoIndicadores.nombreCorto(indicador("euro", 1.0)))
        assertEquals("UF", FormatoIndicadores.nombreCorto(indicador("uf", 1.0)))
        assertEquals("UTM", FormatoIndicadores.nombreCorto(indicador("utm", 1.0)))
        assertEquals("Nombre", FormatoIndicadores.nombreCorto(indicador("ipc", 1.0)))
    }
}
