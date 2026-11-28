package com.cappucare.app.reglas

import com.cappucare.app.model.Indicador
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Cómo se muestran los indicadores de la API externa (formato chileno: 1.234,56). Kotlin puro, probado con JUnit. */
object FormatoIndicadores {

    private val simbolos = DecimalFormatSymbols(Locale("es", "CL")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    /** "$ 950,12" para pesos, "0,40 %" para porcentajes y "US$ 4,12" para dólares. */
    fun valor(indicador: Indicador): String {
        val numero = DecimalFormat("#,##0.00", simbolos).format(indicador.valor)
        return when (indicador.unidad.lowercase(Locale.ROOT)) {
            "porcentaje" -> "$numero %"
            "dólar", "dolar" -> "US$ $numero"
            else -> "$ $numero"
        }
    }

    /** "2026-11-25" -> "25-11-2026". Si no tiene ese formato, se devuelve tal cual. */
    fun fecha(iso: String): String {
        val partes = iso.take(10).split("-")
        return if (partes.size == 3 && partes.all { it.isNotEmpty() }) "${partes[2]}-${partes[1]}-${partes[0]}" else iso
    }

    /** Nombre corto para la tarjeta del Panel. */
    fun nombreCorto(indicador: Indicador): String = when (indicador.codigo) {
        "dolar" -> "Dólar"
        "euro" -> "Euro"
        "uf" -> "UF"
        "utm" -> "UTM"
        else -> indicador.nombre
    }
}
