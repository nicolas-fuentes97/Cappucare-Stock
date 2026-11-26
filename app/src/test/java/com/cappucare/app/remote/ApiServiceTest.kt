package com.cappucare.app.remote

import com.cappucare.app.data.remote.ApiResult
import com.cappucare.app.data.remote.FuenteRemota
import com.cappucare.app.data.remote.RetrofitInstance
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * TODO(E3-9) · Pruebas de la capa remota contra un servidor HTTP simulado (MockWebServer).
 *
 * Cómo se ejecuta: `./gradlew testDebugUnitTest` (JVM, sin emulador). Cuando completes los casos,
 * QUITA la anotación @Ignore de la clase.
 *
 * Ya está resuelto el armado del servidor simulado y UN caso de ejemplo (`loginEnviaUsuarioYClave`).
 * Completa los demás. Cada caso debe comprobar el RESULTADO (`ApiResult`) y, cuando corresponda, lo
 * que el servidor recibió (`servidor.takeRequest()`: método, ruta, cabecera, cuerpo).
 *
 * Casos mínimos (todos con `fail("E3: completar")` hasta que los implementes):
 *   1.  token         · una ruta protegida (GET /productos) sale con "Authorization: Bearer <token>"
 *   2.  token         · login y health salen SIN cabecera Authorization
 *   3.  200 listado   · GET /productos devuelve una lista de modelos Producto
 *   4.  400           · POST /productos con {"error":"...","campo":"codigo"} -> Error(SOLICITUD_INVALIDA, campo = "codigo")
 *   5.  401           · -> Error(NO_AUTORIZADO), mensaje del servidor
 *   6.  403           · -> Error(SIN_PERMISO)
 *   7.  404           · -> Error(NO_ENCONTRADO)
 *   8.  409           · código de producto repetido -> Error(CONFLICTO, campo = "codigo")
 *   9.  422           · stock insuficiente -> Error(REGLA_DE_NEGOCIO) con el mensaje del servidor
 *   10. 500           · cuerpo HTML -> Error(SERVIDOR) con el mensaje genérico de ManejoErrores
 *   11. cuerpo mal formado · 200 con un JSON roto -> Error(RESPUESTA_INVALIDA)
 *   12. sin red       · servidor apagado (`servidor.shutdown()`) -> SinConexion
 *   13. timeout       · `SocketPolicy.NO_RESPONSE` con `segundosLectura = 1` -> SinConexion(porTiempoAgotado = true)
 *   14. movimiento    · POST /movimientos lleva idCliente y NO lleva "fecha"; un ingreso no lleva "motivo"
 */
@Ignore("E3: completar")
class ApiServiceTest {

    private lateinit var servidor: MockWebServer
    private var token: String? = null

    @Before
    fun iniciar() {
        servidor = MockWebServer().also { it.start() }
    }

    @After
    fun cerrar() {
        runCatching { servidor.shutdown() }
    }

    /** FuenteRemota real apuntando al servidor simulado (E3-2 + E3-5). */
    private fun fuente(segundosLectura: Long = 5, segundosTotal: Long = 10) = FuenteRemota {
        RetrofitInstance.crearServicio(
            urlBase = servidor.url("/").toString(),
            segundosLectura = segundosLectura,
            segundosTotal = segundosTotal
        ) { token }
    }

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    // --- Ejemplo resuelto: úsalo como modelo para los demás ---------------------------------------

    @Test
    fun loginEnviaUsuarioYClave() = runTest {
        servidor.enqueue(
            json(
                200,
                """{"token":"abc123","expira":"2030-01-01 00:00",
                    "usuario":{"id":"U1","nombre":"Encargado","usuario":"encargado@cappucare.cl","perfil":"ENCARGADO"}}"""
            )
        )

        val resultado = fuente().login("encargado@cappucare.cl", "encargado123")

        assertTrue(resultado is ApiResult.Success)
        assertEquals("abc123", (resultado as ApiResult.Success).datos.token)
        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/auth/login", peticion.path)
        assertNull(peticion.getHeader("Authorization"))
        assertTrue(peticion.body.readUtf8().contains("encargado123"))
    }

    // --- Para completar ---------------------------------------------------------------------------

    @Test fun rutaProtegidaLlevaTokenBearer() = pendiente()
    @Test fun loginYHealthNoLlevanAuthorization() = pendiente()
    @Test fun productosDevuelveListaDeModelos() = pendiente()
    @Test fun error400LlevaElCampo() = pendiente()
    @Test fun error401EsNoAutorizado() = pendiente()
    @Test fun error403EsSinPermiso() = pendiente()
    @Test fun error404EsNoEncontrado() = pendiente()
    @Test fun error409EsConflictoPorCodigoRepetido() = pendiente()
    @Test fun error422EsReglaDeNegocio() = pendiente()
    @Test fun error500ConHtmlEsErrorDeServidor() = pendiente()
    @Test fun cuerpoMalFormadoEsRespuestaInvalida() = pendiente()
    @Test fun servidorApagadoEsSinConexion() = pendiente()
    @Test fun sinRespuestaATiempoEsSinConexionPorTiempoAgotado() = pendiente()
    @Test fun registrarMovimientoEnviaIdClienteYOmiteFechaYMotivoEnIngresos() = pendiente()

    private fun pendiente() {
        fail("E3: completar")
    }
}
