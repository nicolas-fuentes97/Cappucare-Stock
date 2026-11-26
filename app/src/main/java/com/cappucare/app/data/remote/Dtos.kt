package com.cappucare.app.data.remote

/*
 * DTOs: la forma EXACTA de los JSON de la API (backend/docs/API.md). Los nombres de campo son los
 * mismos que usa el servidor. Las respuestas tienen campos anulables porque Gson crea los objetos
 * sin pasar por el constructor de Kotlin: si el servidor omitiera un campo, quedaría en null. Los
 * mapeadores (MapeadoresRemotos.kt) revisan cada campo y convierten a los modelos del dominio.
 * Los enums viajan como texto (enum.name), igual que en Room.
 *
 * backend/scripts/comparar-dtos.js compara estos nombres con data/seed.json y con lo que
 * responde el servidor (npm run verificar:dtos).
 */

data class LoginRequestDto(
    val usuario: String,
    val clave: String
)

data class UsuarioDto(
    val id: String?,
    val nombre: String?,
    val usuario: String?,
    val perfil: String?
)

data class LoginRespuestaDto(
    val token: String?,
    val expira: String?,
    val usuario: UsuarioDto?
)

data class ProductoDto(
    val id: String?,
    val codigo: String?,
    val nombre: String?,
    val descripcion: String?,
    val categoria: String?,
    val unidadMedida: String?,
    val cantidadDisponible: Int?,
    val nivelMinimo: Int?,
    val ubicacion: String?,
    val estado: String?
)

/** Cuerpo de POST /productos y PUT /productos/{id}: la cantidad y el estado no se envían (RN-01). */
data class ProductoRequestDto(
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val unidadMedida: String,
    val nivelMinimo: Int,
    val ubicacion: String
)

data class EstadoRequestDto(
    val estado: String
)

data class MovimientoDto(
    val id: String?,
    val productoId: String?,
    val fecha: String?,
    val tipo: String?,
    val cantidad: Int?,
    val motivo: String?,
    val observacion: String?,
    val responsable: String?,
    val responsablePerfil: String?
)

/** Cuerpo de POST /movimientos. [idCliente] evita duplicados si el envío se repite. */
data class MovimientoRequestDto(
    val idCliente: String,
    val productoId: String,
    val tipo: String,
    val cantidad: Int,
    val motivo: String?,
    val observacion: String,
    val fecha: String?
)

data class MovimientoRespuestaDto(
    val movimiento: MovimientoDto?,
    val producto: ProductoDto?,
    val productoAntes: ProductoDto?,
    val repetido: Boolean?
)

data class HealthDto(
    val status: String?,
    val servicio: String?,
    val version: String?,
    val hora: String?
)

data class OkDto(
    val ok: Boolean?
)

/** Cuerpo de error de la API: `{error, campo?}`. */
data class ErrorDto(
    val error: String?,
    val campo: String?
)
