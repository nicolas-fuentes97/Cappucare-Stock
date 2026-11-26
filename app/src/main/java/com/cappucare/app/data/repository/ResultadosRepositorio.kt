package com.cappucare.app.data.repository

import com.cappucare.app.model.Producto
import com.cappucare.app.model.Usuario

/** Producto antes y después de un movimiento (para decidir, por ejemplo, si avisar stock mínimo). */
data class ResultadoMovimiento(val antes: Producto, val despues: Producto)

/** Resultado de iniciar sesión contra la API (E3-7). */
sealed class ResultadoLogin {
    /** [sinConexion] = true si se ingresó con los usuarios guardados en el equipo porque no había servidor. */
    data class Exito(val usuario: Usuario, val sinConexion: Boolean = false) : ResultadoLogin()

    /** El servidor rechazó las credenciales (401): [mensaje] genérico, no dice cuál de los dos falló. */
    data class Rechazado(val mensaje: String) : ResultadoLogin()

    /** No se pudo ingresar por otra causa (servidor caído sin usuario guardado, 5xx, respuesta inválida...). */
    data class Fallo(val mensaje: String) : ResultadoLogin()
}

