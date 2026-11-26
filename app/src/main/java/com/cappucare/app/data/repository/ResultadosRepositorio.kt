package com.cappucare.app.data.repository

import com.cappucare.app.model.Producto
import com.cappucare.app.model.Usuario

/** Producto antes y después de un movimiento (para decidir, por ejemplo, si avisar stock mínimo). */
data class ResultadoMovimiento(val antes: Producto, val despues: Producto)

/** Resultado de iniciar sesión (RF-00). */
sealed class ResultadoLogin {

    data class Exito(val usuario: Usuario) : ResultadoLogin()

    /** Credenciales incorrectas: [mensaje] genérico, no dice cuál de los dos falló. */
    data class Rechazado(val mensaje: String) : ResultadoLogin()
}
