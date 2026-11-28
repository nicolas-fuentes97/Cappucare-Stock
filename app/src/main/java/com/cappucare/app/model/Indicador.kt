package com.cappucare.app.model

/**
 * Indicador económico del día (API externa mindicador.cl). Se muestra en el Panel como apoyo a
 * las compras de reposición: muchos insumos médicos se cotizan en dólares o en euros y los
 * contratos con proveedores suelen expresarse en UF.
 *
 * @property codigo "dolar", "euro", "uf" o "utm".
 * @property unidad unidad de medida que informa la API ("Pesos", "Porcentaje"...).
 * @property fecha día del valor, en formato "yyyy-MM-dd".
 */
data class Indicador(
    val codigo: String,
    val nombre: String,
    val valor: Double,
    val unidad: String,
    val fecha: String
)
