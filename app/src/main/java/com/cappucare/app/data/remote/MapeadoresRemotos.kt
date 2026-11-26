package com.cappucare.app.data.remote

import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Perfil
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import com.cappucare.app.model.Usuario

/*
 * TODO(E3-4) · Mapeadores DTO <-> modelo del dominio.
 *
 * Cada DTO (Dtos.kt) es la forma EXACTA del JSON de la API; el modelo (model/) es lo que usa el
 * resto de la app. Gson crea los DTO sin pasar por el constructor de Kotlin, así que un campo
 * que el servidor omita llega como null aunque el tipo parezca obligatorio.
 *
 * QUÉ HACER: implementar las funciones de este archivo (cada una tiene su TODO). Reglas:
 *  - Si falta un campo obligatorio o un enum trae un valor que la app no conoce, lanzar
 *    RespuestaInvalidaException (ManejoErrores la convierte en un mensaje para el usuario).
 *    Los ayudantes [requerido] y [enumDe] (abajo, ya resueltos) hacen justo eso.
 *  - Los enums viajan como texto: `enum.name`.
 *  - La API nunca entrega claves: en una sesión remota, Usuario.claveHash queda "".
 *  - En el cuerpo para crear o editar un producto NO se envían id, cantidad ni estado (RN-01).
 *  - `descripcion` y `observacion` pueden venir null: se mapean a "".
 *  - `motivo` puede ser null (un ingreso no tiene motivo).
 *
 * CRITERIO DE ACEPTACIÓN: las pruebas de E3-10 (ida y vuelta de cada DTO, campo ausente, enum
 * desconocido, motivo nulo, sin id/cantidad/estado en el cuerpo del producto).
 */

/** (Ya resuelto.) Devuelve [valor] o lanza RespuestaInvalidaException si es null. */
internal fun <T : Any> requerido(valor: T?, campo: String): T =
    valor ?: throw RespuestaInvalidaException("Falta el campo \"$campo\" en la respuesta del servidor")

/** (Ya resuelto.) Convierte el texto al enum [E] o lanza RespuestaInvalidaException. */
internal inline fun <reified E : Enum<E>> enumDe(valor: String?, campo: String): E {
    val texto = requerido(valor, campo)
    return enumValues<E>().firstOrNull { it.name == texto }
        ?: throw RespuestaInvalidaException("Valor desconocido \"$texto\" en el campo \"$campo\"")
}

// --- Usuario y sesión -----------------------------------------------------------------------

fun UsuarioDto.aModelo(): Usuario {
    TODO("E3-4: Usuario(id, nombre, usuario, claveHash = \"\", perfil = enumDe<Perfil>(...))")
}

/** Sesión que devuelve POST /auth/login. */
data class SesionRemotaDatos(val token: String, val usuario: Usuario)

fun LoginRespuestaDto.aSesion(): SesionRemotaDatos {
    TODO("E3-4: token obligatorio y no vacío + usuario.aModelo()")
}

// --- Productos ------------------------------------------------------------------------------

fun ProductoDto.aModelo(): Producto {
    TODO("E3-4: Producto con CategoriaInsumo, UnidadMedida y EstadoProducto desde texto (enumDe)")
}

/** Cuerpo para crear o editar: sin id, cantidad ni estado (RN-01: los decide el servidor). */
fun Producto.aRequest(): ProductoRequestDto {
    TODO("E3-4: ProductoRequestDto con codigo/nombre/descripcion/ubicacion recortados y enums como name")
}

fun EstadoProducto.aRequest(): EstadoRequestDto {
    TODO("E3-4: EstadoRequestDto(name)")
}

// --- Movimientos ----------------------------------------------------------------------------

fun MovimientoDto.aModelo(): Movimiento {
    TODO("E3-4: Movimiento con TipoMovimiento, motivo opcional (MotivoSalida) y responsablePerfil (Perfil)")
}

/**
 * Cuerpo de POST /movimientos. [idCliente] es un identificador único que genera la app para que,
 * si el envío se repite, el servidor no lo registre dos veces. [motivo] solo se envía en las
 * salidas (RN-02). La fecha la pone el servidor (RN-05): se envía null.
 */
fun crearRequestMovimiento(
    idCliente: String,
    productoId: String,
    tipo: TipoMovimiento,
    cantidad: Int,
    motivo: MotivoSalida?,
    observacion: String
): MovimientoRequestDto {
    TODO("E3-4: MovimientoRequestDto(idCliente, productoId, tipo.name, cantidad, motivo solo si es SALIDA, observacion, fecha = null)")
}

/** Resultado de registrar un movimiento: el movimiento guardado y el producto antes y después. */
data class RespuestaMovimiento(val movimiento: Movimiento, val antes: Producto, val despues: Producto)

fun MovimientoRespuestaDto.aModelo(): RespuestaMovimiento {
    TODO("E3-4: movimiento, productoAntes y producto son obligatorios; mapear cada uno")
}
