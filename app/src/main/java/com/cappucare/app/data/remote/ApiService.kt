package com.cappucare.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Todas las rutas de la API (backend/docs/API.md). Son funciones suspend: Retrofit las ejecuta
 * fuera del hilo principal y, si el servidor responde con un código de error, lanzan
 * HttpException (ManejoErrores la convierte en un mensaje para el usuario). Las rutas, salvo
 * /health y /auth/login, necesitan el token: lo agrega InterceptorToken.
 *
 * Las rutas no empiezan con "/" porque se suman a la dirección base del servidor.
 */
interface ApiService {

    /** Sin autenticación: sirve para "Probar conexión". */
    @GET("health")
    suspend fun health(): HealthDto

    // --- RF-00 · Acceso ---------------------------------------------------------------------

    @POST("auth/login")
    suspend fun login(@Body cuerpo: LoginRequestDto): LoginRespuestaDto

    @POST("auth/logout")
    suspend fun logout(): OkDto

    @GET("usuarios/me")
    suspend fun usuarioActual(): UsuarioDto

    // --- RF-01, RF-02, RF-03, RF-08, RF-09 · Productos ---------------------------------------

    @GET("productos")
    suspend fun productos(
        @Query("q") texto: String? = null,
        @Query("categoria") categoria: String? = null,
        @Query("estado") estado: String? = null,
        @Query("bajoMinimo") bajoMinimo: Boolean? = null
    ): List<ProductoDto>

    @GET("productos/reposicion")
    suspend fun reposicion(): List<ProductoDto>

    @GET("productos/codigo/{codigo}")
    suspend fun productoPorCodigo(@Path("codigo") codigo: String): ProductoDto

    @GET("productos/{id}")
    suspend fun producto(@Path("id") id: String): ProductoDto

    @POST("productos")
    suspend fun crearProducto(@Body cuerpo: ProductoRequestDto): ProductoDto

    @PUT("productos/{id}")
    suspend fun editarProducto(@Path("id") id: String, @Body cuerpo: ProductoRequestDto): ProductoDto

    @PATCH("productos/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: String, @Body cuerpo: EstadoRequestDto): ProductoDto

    // --- RF-04, RF-05, RF-06, RF-07, RF-10 · Movimientos -------------------------------------

    @GET("movimientos")
    suspend fun movimientos(
        @Query("productoId") productoId: String? = null,
        @Query("tipo") tipo: String? = null
    ): List<MovimientoDto>

    @POST("movimientos")
    suspend fun registrarMovimiento(@Body cuerpo: MovimientoRequestDto): MovimientoRespuestaDto
}
