# Cappucare Stock — Control inteligente y oportuno de existencias

Aplicación Android (Kotlin · Jetpack Compose · Material 3 · MVVM · Room · Retrofit) para el caso de **Cappucare**, organización dedicada a la venta de productos e insumos médicos. Permite controlar existencias, disponibilidad y movimientos (recepción, salidas y ventas), consultar el stock de cualquier producto y anticipar la reposición cuando un producto queda bajo su nivel mínimo.

**Asignatura:** DSY1105 – Desarrollo de Aplicaciones Móviles · **Versión:** 3.1 (`versionCode` 4) · **Datos:** íntegramente ficticios (RNF-04), sin conexión a sistemas reales (RNF-05).

Este proyecto tiene dos modos de ejecución. **La bandera `USAR_API` de `gradle.properties` solo define el modo con el que parte la app**; después se cambia en ejecución desde **Login > Servidor** (ver "Modo local y modo servidor"):

| Modo | `USAR_API` (modo inicial) | Qué hace |
|---|---|---|
| **Local** (por defecto) | `false` | Funciona completo **sin servidor**, solo con Room: RF-00 a RF-10, reglas de negocio, cámara QR, notificación y exportación a CSV. Es la línea base funcional del caso. |
| **API** | `true` | La app usa el servidor (`backend/`, entregado aparte) como **fuente de verdad** y Room como copia local. Esa parte la completas tú resolviendo los `TODO(E3-n)` (ver "Experiencia 3: integración con la API"). |

Mientras no resuelvas los `TODO`, **deja `USAR_API=false`**: con `true` (o activando el modo servidor en la app) la app se detiene en el primer `TODO(...)` que encuentre.

Material del caso: `docs/qr/` contiene los códigos QR de prueba de los 8 productos de ejemplo (solo fines académicos).

---

## Perfiles y usuarios de prueba

| Perfil | Usuario | Clave | Puede |
|---|---|---|---|
| Personal encargado | `encargado@cappucare.cl` | `encargado123` | Todo: crear y editar productos, cambiar su estado, registrar movimientos, consultar, ver alertas e historial |
| Usuario autorizado | `ventas@cappucare.cl` | `ventas123` | Consultar disponibilidad, registrar ingresos y salidas, ver alertas e historial. **No** crea ni edita productos ni cambia su estado (RN-08, supuesto SUP-06) |

Las claves de prueba nunca se guardan en texto plano: la base almacena su resumen SHA-256 con sal (`reglas/Seguridad.kt`). El login es real (no existe modo de acceso alternativo) y el nombre y perfil del usuario con sesión quedan registrados como responsable de cada movimiento.

## Datos de ejemplo

Se cargan una sola vez al crear la base: 2 usuarios, 8 productos (P1 a P8, códigos `CUR-001`, `CUR-002`, `PRO-001`, `PRO-002`, `MED-001`, `MED-002`, `HIG-001`, `ORT-001`) y 11 movimientos (M1 a M11). Los movimientos están **cuadrados con el stock**: para cada producto, ingresos menos salidas es igual a su cantidad disponible (RN-01), y una prueba unitaria lo verifica. Con estos datos las alertas de reposición muestran P2 (8 de 15), P4 (3 de 10) y P6 (0 de 3).

---

## Requerimientos funcionales

La columna "Estado" describe el modo local. En modo API, cada RF pasa por el servidor (tabla de endpoints en `backend/docs/API.md`).

| Código | Requerimiento | Estado | Dónde |
|---|---|---|---|
| RF-00 | Inicio de sesión del personal (campos obligatorios, credenciales, mensaje genérico, cerrar sesión, recordar el último usuario con DataStore) | Implementado | `LoginScreen`, `CappucareViewModel.login()/logout()`, `Validaciones.validarLogin()`, `Seguridad`, `PreferenciasSesion` |
| RF-01 | Ingresar y editar un producto (código, nombre, descripción, categoría, unidad, nivel mínimo, ubicación) con validación por campo y código único | Implementado | `ProductoFormScreen`, `Validaciones.validarProducto()`, `FuenteLocal.guardarProducto()` (modo local) |
| RF-02 | Clasificar y filtrar productos por categoría | Implementado | `ProductosScreen` (selector de categoría), `ReglasInventario.filtrarProductos()` |
| RF-03 | Actualizar el estado de un producto (confirmación antes de "Dado de baja"; el estado debe ser coherente con la cantidad) | Implementado | `ProductoDetalleScreen`, `ReglasInventario.validarCambioEstado()`, `CappucareViewModel.cambiarEstado()` |
| RF-04 | Registrar un ingreso (recepción de mercadería) con cantidad y fecha | Implementado | `RegistrarMovimientoScreen`, `Validaciones.validarMovimiento()`, `FuenteLocal.registrarMovimiento()` (modo local) |
| RF-05 | Registrar una salida (incluidas las ventas) con cantidad, motivo y fecha | Implementado | `RegistrarMovimientoScreen` (motivo con chips), `Validaciones.validarMovimiento()` |
| RF-06 | Registrar el responsable (nombre y perfil) de cada movimiento, tomado de la sesión | Implementado | `CappucareViewModel.registrarMovimiento()`, `Movimiento.responsable/responsablePerfil`, `HistorialMovimientosScreen` |
| RF-07 | Actualizar automáticamente la cantidad disponible (y pasar a Agotado / Disponible al llegar a 0 o reponer) | Implementado | `ReglasInventario.aplicarMovimiento()`, transacción en `FuenteLocal.registrarMovimiento()` (modo local) |
| RF-08 | Consultar la disponibilidad de cualquier producto: búsqueda por nombre o código mientras se escribe, o escaneando su QR | Implementado | `ProductosScreen`, `ProductoDetalleScreen`, `BotonEscanearCodigo`, `ReglasInventario.buscarPorCodigo()` |
| RF-09 | Identificar productos bajo su nivel mínimo (alertas, contador del Panel, marca en la lista, aviso en el detalle) | Implementado | `ReglaReposicion`, `ReposicionScreen`, `PanelScreen`, `ProductosScreen`, `ProductoDetalleScreen` |
| RF-10 | Historial comprensible por producto o general, del más reciente al más antiguo, con saldo, resumen y filtro por tipo; exportable a CSV | Implementado | `ReglasHistorial`, `HistorialMovimientosScreen` |

### Reglas de negocio

| Código | Regla | Cómo se cumple |
|---|---|---|
| RN-01 | La cantidad disponible se deriva de los movimientos | El formulario de producto no tiene cantidad; solo cambia en la transacción de `registrarMovimiento()`; al editar se conserva |
| RN-02 | Toda salida tiene motivo; las ventas, motivo "Venta" | `Validaciones.validarMovimiento()` ("Elige el motivo de la salida"); el motivo solo se guarda en salidas |
| RN-03 | El stock nunca queda negativo | Validación en el formulario ("Solo hay N disponibles") y segunda barrera en el repositorio, dentro de la transacción |
| RN-04 | El código de producto es único | Validación sin distinguir mayúsculas + índice único en Room + código guardado en mayúsculas |
| RN-05 | Todo movimiento tiene responsable, fecha y hora | Los asigna el ViewModel/repositorio; no se escriben a mano |
| RN-06 | Los movimientos no se editan ni se eliminan | El DAO de movimientos solo tiene `@Insert` y consultas; la pantalla es de solo lectura |
| RN-07 | "Bajo el mínimo" = disponible **menor estricto** que el mínimo, sin los dados de baja | Una sola implementación (`ReglaReposicion.estaBajoMinimo`) para todas las pantallas |
| RN-08 | Permisos según el perfil | `Perfil.puedeEditarCatalogo`; se ocultan las acciones y el ViewModel lo vuelve a verificar ("No tienes permiso para realizar esta acción.") |

### Requisitos no funcionales y restricciones

| Código | Estado | Detalle |
|---|---|---|
| RNF-01 / RT-02 | Implementado | Room: los datos sobreviven al cierre de la app y funcionan sin conexión |
| RNF-02 | Implementado | Producto preseleccionado desde el detalle; registrar una venta son 4 toques más la cantidad; búsqueda en vivo; errores bajo cada campo |
| RNF-03 | Implementado | Movimiento y cantidad en una sola `@Transaction` (`withTransaction`) |
| RNF-04 / RNF-05 | Implementado | Solo datos ficticios; sin URLs ni credenciales de sistemas reales; el acceso a datos está en el repositorio |
| RNF-06 (opcional) | Implementado | Cámara con ZXing para leer QR o código de barras; el permiso se pide al tocar el ícono (con el catálogo guardado en el equipo, también sin conexión) |
| RNF-07 (opcional) | Implementado | Notificación local solo cuando un movimiento hace cruzar el mínimo; `POST_NOTIFICATIONS` en Android 13+ desde Alertas de reposición |
| RNF-08 | Implementado | Clave oculta por defecto, claves como SHA-256 con sal, cierre de sesión que limpia la pila y vuelta al login si la app se restaura sin sesión |
| RNF-09 | Implementado | minSdk 24; Room fuera del hilo principal (`suspend`/`Flow`); listas con `LazyColumn`; una prueba verifica la búsqueda con 200 productos |
| RNF-10 / RT-01 | Implementado | MVVM: un ViewModel, un repositorio, reglas en Kotlin puro; pantallas que solo dibujan |

### Recursos nativos

1. **Cámara** (QR y códigos de barras): buscar o elegir un producto sin escribir.
2. **Notificaciones locales** con permiso en tiempo de ejecución: aviso de reposición.
3. **Selector de archivos del sistema (SAF)**: exportar el historial a CSV sin permisos de almacenamiento.

### Supuestos y decisiones de diseño (pendientes de confirmar con el CITT)

- Categorías, estados (disponible, agotado, vencido, dado de baja) y motivos de salida son propuestas ajustables en los enums de `model/`.
- Un solo punto de almacenamiento (SUP-05); nivel mínimo fijo por producto (SUP-07); datos solo en el dispositivo (SUP-08).
- Al llegar a 0 un producto Disponible pasa a Agotado y vuelve a Disponible con un ingreso; Vencido y Dado de baja no cambian solos.
- No se vende un producto Vencido o Dado de baja; un producto Dado de baja no recibe ingresos (sí salidas de ajuste, merma, vencimiento u otro).
- Agotado solo se puede elegir a mano con 0 unidades y Disponible solo con al menos 1.

---

## Arquitectura

```
Pantalla (Compose) → CappucareViewModel → SelectorFuente ─┬→ FuenteLocal ────→ Room "cappucare_local.db"        MODO LOCAL
      ↑ collectAsState()     StateFlow       (modo vigente)    └→ FuenteServidor → CappucareRepository ─┬→ DAO → Room "cappucare.db" (copia)
                                  │                                                                    └→ FuenteRemota → ApiService (Retrofit/OkHttp) → API   MODO SERVIDOR
                                  └── reglas/ (Kotlin puro, probadas con JUnit; las usan ambos modos)
```

En modo servidor la **API es la fuente de verdad** y Room es una **copia local**: las pantallas siguen observando Room; el repositorio la renueva con lo que responde el servidor, y toda escritura va primero al servidor.

Paquete base `com.cappucare.app`:

| Paquete | Contenido |
|---|---|
| `model/` | `Producto`, `Movimiento`, `Usuario`, `Perfil`, enums y clases de errores de formulario |
| `reglas/` | Reglas de negocio y validaciones en Kotlin puro: `Validaciones`, `ReglasInventario`, `ReglaReposicion`, `ReglasHistorial`, `Seguridad`, `UrlServidor` (EA3, con `TODO`) |
| `data/` | `MockData` (datos ficticios de ejemplo) y `ModoOperacion` (modo local / servidor vigente, en memoria) |
| `data/local/` | Room: `Entidades`, `Daos`, `AlmacenLocal`/`AlmacenRoom` (base del modo local), `AppDatabase` (versión 1, carga inicial; dos archivos: servidor y local), `Mapeos`; DataStore: `PreferenciasSesion`, `PreferenciasServidor`, `PreferenciasModo` |
| `data/remote/` | Capa remota (EA3): `Dtos` y `ApiService` (resueltos: son el contrato con el servidor), `ApiResult`, `SesionRemota`, `ErroresServidor` (resueltos) y las clases con `TODO`: `InterceptorToken`, `RetrofitInstance`, `ManejoErrores`, `MapeadoresRemotos`, `FuenteRemota`, `PruebaServidor` |
| `data/repository/` | `FuenteDatos` (interfaz única que usa el ViewModel), `FuenteLocal` (modo local: reglas de negocio sobre Room, transacción movimiento + stock), `FuenteServidor` + `CappucareRepository` (modo servidor: métodos `...Remoto` y `sincronizar`, EA3, con `TODO`) y `SelectorFuente` (elige según el modo vigente) |
| `viewmodel/` | `CappucareViewModel`: estado con StateFlow, validación y mensajes al usuario |
| `navigation/` | Rutas, NavHost con transiciones animadas, Snackbar global y guardia de sesión |
| `notificacion/` | Notificación de stock mínimo |
| `ui/components/`, `ui/screen/`, `ui/theme/` | Componentes reutilizables, pantallas por módulo (`productos/`, `movimientos/`, `reposicion/`) y tema Material 3 |

Hay dos archivos de base con las mismas tablas y la misma carga inicial: `cappucare_local.db` (modo local) y `cappucare.db` (copia del servidor). Tablas: `usuarios` (índice único en `usuario`), `productos` (índice único en `codigo`) y `movimientos` (clave foránea a `productos`).

Flujo de pantallas:

```
LOGIN ──(login OK)──> PANEL
PANEL ──> PRODUCTOS ──> DETALLE ──> FORMULARIO (editar)
                    │           ├─> REGISTRAR MOVIMIENTO
                    │           └─> HISTORIAL (de ese producto)
                    └─> FORMULARIO (nuevo, botón +)
PANEL ──> REGISTRAR MOVIMIENTO | ALERTAS DE REPOSICIÓN | HISTORIAL (todos)
Cerrar sesión ──> LOGIN
```

---

## Cómo ejecutar

1. Android Studio (Koala o superior) con **JDK 17 o 21** como Gradle JVM.
2. **File > Open…** y seleccionar la carpeta que contiene `settings.gradle.kts`. Esperar el Gradle Sync (la primera vez necesita internet).
3. Ejecutar ▶ **Run 'app'** en un emulador o teléfono con Android 7.0 (API 24) o superior.
4. Ingresar con alguno de los usuarios de prueba (modo local: `USAR_API=false`). La franja superior indica "MODO LOCAL".

Si Android Studio sugiere actualizar Gradle o el Android Gradle Plugin, **no aceptar**: las versiones están fijadas (Kotlin 2.0.0, KSP 2.0.0-1.0.24, AGP 8.5.2, Gradle 8.7, Room 2.6.1, Compose BOM 2024.09.00).

Para probar la cámara usar `docs/qr/hoja_QR_productos.png` (o los PNG individuales) en pantalla. En un emulador, configurar la cámara virtual para leer una imagen.

Si cambia el esquema de Room durante el desarrollo: subir `version` en `AppDatabase` y agregar la migración, o desinstalar la app del dispositivo.

## Modo local y modo servidor

La app tiene dos modos de operación y se cambia **en cualquier momento, sin reiniciar**, desde **Login > Servidor** con el interruptor **"Modo local (sin servidor)"**. La elección queda guardada en el equipo (DataStore, preferencia `modo_local`).

> **`USAR_API` solo define el modo inicial** (`false` = local, `true` = servidor) mientras nadie haya elegido otro en la app. Después manda la preferencia guardada.

| Modo | Datos | Para qué sirve |
|---|---|---|
| **Local** (inicial con `USAR_API=false`) | Solo Room, en `cappucare_local.db`, sin red | Funciona completo sin servidor; **respaldo** para demostrar la app si el servicio no está disponible |
| **Servidor** (inicial con `USAR_API=true`) | La API es la fuente de verdad (lo que implementas en los `TODO(E3-n)`); Room (`cappucare.db`) es su copia | Es el modo que se **demuestra en la defensa** |

Cómo funciona:

- Una única interfaz, `FuenteDatos`, es la que usa el ViewModel. `SelectorFuente` consulta el modo vigente (`ModoOperacion`) **en cada operación** y delega en `FuenteLocal` (modo local) o en `FuenteServidor` (adapta `CappucareRepository`, donde están tus `TODO`). Los flujos de productos e historial también cambian de origen al cambiar el modo.
- `FuenteLocal` **no duplica reglas**: usa las mismas funciones puras de `reglas/` (`Validaciones`, `ReglasInventario`, `Seguridad`). Todos los RF funcionan con escritura. Sus errores usan el mismo tipo y campo que los del servidor (código repetido = `CONFLICTO` en `codigo`; salida mayor al stock = `REGLA_DE_NEGOCIO` en `cantidad`).
- **Login local:** busca el usuario en Room y compara el SHA-256 con la misma sal que usa el servidor. En modo servidor el login es `POST /auth/login` (E3-7).
- **Indicador:** una franja en la parte superior de todas las pantallas dice **"MODO LOCAL"** o **"MODO SERVIDOR"**.
- Si **Probar conexión** falla, el diálogo ofrece **"Usar modo local"**; en modo local se muestra **"Restablecer datos locales"** (vuelve a los 8 productos y 11 movimientos de ejemplo).
- **Los datos de los dos modos son independientes:** lo creado en modo local **no se envía al servidor ni se sincroniza** al volver al modo servidor (y viceversa). Al cambiar de modo se cierra la sesión.
- Mientras no hayas resuelto los `TODO`, **el interruptor sigue funcionando** (es independiente), pero activar el modo servidor detiene la app en el primer `TODO(...)`. En el diálogo, "Probar conexión" y "Guardar" también dependen de los `TODO(E3-8)`.

---

## Experiencia 3: integración con la API

### Qué viene resuelto y qué debes completar

Vienen resueltos los DTO (`data/remote/Dtos.kt`), `ApiService` con todas las rutas, `SesionRemota`, `FabricaApi`, el diálogo "Servidor", los DAO de la copia local, la integración en `CappucareViewModel` y las pantallas. Debes completar los `TODO(E3-n)`; cada uno indica **qué hacer, en qué archivo y cómo se comprueba**:

| TODO | Archivo | Qué se construye |
|---|---|---|
| E3-1 | `data/remote/InterceptorToken.kt` | Encabezado `Authorization: Bearer` en cada petición protegida |
| E3-2 | `data/remote/RetrofitInstance.kt` | `OkHttpClient` (timeouts, interceptor, registro HTTP en debug) y `Retrofit` con Gson |
| E3-3 | `data/remote/ManejoErrores.kt` | Excepciones y códigos HTTP a `ApiResult` con mensajes en español |
| E3-4 | `data/remote/MapeadoresRemotos.kt` | DTO a modelo y modelo a cuerpo de solicitud |
| E3-5 | `data/remote/FuenteRemota.kt` | Fuente de datos remota (única clase que llama a `ApiService`) |
| E3-6a–d | `data/repository/CappucareRepository.kt` | API como fuente de verdad + Room como copia local: sincronizar, crear/editar producto, cambiar estado, registrar movimiento |
| E3-7a–b | `data/repository/CappucareRepository.kt` | Login contra `/auth/login` (con respaldo sin conexión) y cierre de sesión |
| E3-8a–c | `reglas/UrlServidor.kt`, `data/remote/PruebaServidor.kt`, `data/local/PreferenciasServidor.kt` | Dirección del servidor: validación, "Probar conexión" a `/health` y persistencia |
| E3-9 a E3-12 | `src/test/…/remote`, `src/test/…/repository`, `src/androidTest/…` | Pruebas de la capa remota, de mapeadores y validación de URL, del repositorio y de la interfaz (Compose) |

Para ver todos los pendientes: en Android Studio, **View > Tool Windows > TODO** (o buscar `TODO(E3`).

### Conexión con el servidor

1. Levantar el servidor (carpeta `backend/` entregada aparte): `npm install && npm start` (Node 20 o superior). Responde en `http://localhost:3000/health`.
2. Poner `USAR_API=true` en `gradle.properties` y sincronizar Gradle (la app parte en modo servidor), o dejar `false` y desactivar "Modo local" en **Login > Servidor**.
3. Elegir la dirección según dónde corra la app:

| App en… | Dirección |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:3000/` (valor por defecto de `API_BASE_URL`) |
| Teléfono en la misma red Wi-Fi | `http://IP-del-PC:3000/` (por ejemplo `http://192.168.1.20:3000/`) |
| Teléfono por Internet (Port Forwarding de VS Code, visibilidad **Pública**) | `https://…` que entrega VS Code |

La dirección inicial sale de `API_BASE_URL` (`gradle.properties`) y se puede cambiar **sin recompilar** desde **Login > Servidor** (valida la dirección, permite "Probar conexión" y la guarda en el equipo).

Solución de problemas: texto plano (`http`) solo se acepta hacia el emulador, `localhost` y redes locales; para Internet usa `https`. Si un teléfono no llega al PC, revisa el firewall del PC (puerto 3000) y que ambos estén en la misma red. Con Port Forwarding en visibilidad privada, VS Code devuelve una página HTML de acceso en lugar de JSON: cambia la visibilidad a Pública.

### Usuarios y datos

Los mismos usuarios de prueba y los mismos 8 productos de ejemplo existen en el servidor (`backend/data/seed.json`). `npm run reset` en `backend/` restaura los datos del servidor.

## Cómo correr las pruebas

**Pruebas de reglas (ya entregadas)** en `app/src/test/java/com/cappucare/app/reglas/`: validaciones, cantidad automática, estados, búsqueda, historial y CSV, alertas de reposición, seguridad e integridad de los datos de ejemplo (80 pruebas, JUnit 4). Funcionan sin servidor.

**Pruebas del modo local (ya entregadas, activas)**: `src/test/…/modo/ModoOperacionTest.kt`, `src/test/…/repository/FuenteLocalTest.kt` (login con hash, escrituras y reglas de negocio con sus errores) y `src/test/…/repository/SelectorFuenteTest.kt` (el selector elige la fuente según el modo, cambio de modo en caliente e independencia de los datos). Usan una base en memoria y no necesitan servidor ni emulador.

**Pruebas de la Experiencia 3 (por completar)**, con `@Ignore("E3: completar")` y la lista de casos en el comentario de cada archivo (mínimo 15 casos, incluyendo 400/401/403/404/409, sin red, tiempo agotado, cuerpo mal formado, mapeadores y copia local):

| Archivo | Qué prueba |
|---|---|
| `src/test/…/remote/ApiServiceTest.kt` | La capa remota contra un servidor simulado (MockWebServer) |
| `src/test/…/remote/ManejoErroresYMapeadoresTest.kt` | Errores, mapeadores y validación de la dirección (Kotlin puro) |
| `src/test/…/repository/CappucareRepositoryRemotoTest.kt` | Repositorio con fuente remota falsa (MockK) y base falsa |
| `src/androidTest/…/ui/LoginScreenTest.kt` | Interfaz Compose en emulador o teléfono (3 casos) |

- **Android Studio:** clic derecho en la carpeta `test` (o en un archivo) > **Run Tests**. Las de interfaz: clic derecho en `androidTest`.
- **Terminal:** `./gradlew testDebugUnitTest` (JVM, sin emulador) y `./gradlew connectedDebugAndroidTest` (con emulador o teléfono). El reporte queda en `app/build/reports/tests/`.

## Cómo generar el APK firmado

El proyecto **no incluye ninguna llave ni credencial** (`keystore.properties.example` solo muestra los nombres de los datos de firma). El build `release` funciona sin firma (genera un APK sin firmar); si se definen los datos de firma, se firma automáticamente.

1. Crear una llave propia (una sola vez, exclusiva para esta entrega):
   ```
   keytool -genkeypair -v -keystore cappucare-release.jks -alias cappucare -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Definir los datos de firma en `~/.gradle/gradle.properties` (archivo personal, no el del proyecto) o como variables de entorno con los mismos nombres:
   ```
   CAPPUCARE_STORE_FILE=/ruta/a/cappucare-release.jks
   CAPPUCARE_STORE_PASSWORD=<clave del almacén>
   CAPPUCARE_KEY_ALIAS=cappucare
   CAPPUCARE_KEY_PASSWORD=<clave de la llave>
   ```
3. Generar: **Build > Generate Signed App Bundle / APK…** en Android Studio, o `./gradlew assembleRelease`. El APK queda en `app/build/outputs/apk/release/`.
4. Verificar la firma: `apksigner verify --print-certs app-release.apk` (el procedimiento completo, con firmas V1 y V2 y la bitácora, está en la Parte 6 del enunciado de la actividad).

`.gitignore` excluye `local.properties`, `build/`, `.gradle/`, `.idea/` y `.kotlin/`. Para la evaluación, el `.jks` (llave exclusiva de esta entrega), el APK firmado y su configuración van en el repositorio; no reutilices esa llave en otra app. La minificación está desactivada (`isMinifyEnabled = false`).

Antes de generar el APK de la Experiencia 3, cambia `USAR_API=true` (modo inicial = servidor) y comprueba la app contra el servidor. En la defensa se demuestra el **modo servidor**; el modo local es solo un respaldo. El APK instalado usa la dirección de `API_BASE_URL` o la que guardes en **Login > Servidor**.

## Puntos de extensión

- **Persistir y sincronizar movimientos sin conexión:** hoy, sin conexión, las escrituras se rechazan con un mensaje claro y las lecturas usan la copia local. Una cola de pendientes con un identificador de idempotencia (`idCliente`, que el servidor ya admite) permitiría registrar movimientos sin red y enviarlos después.
- **Filtro por rango de fechas del historial (RF-10, opcional):** no está implementado; el servidor ya entrega la fecha de cada movimiento.
- **Más pruebas:** las reglas están en Kotlin puro y se prueban sin emulador; el repositorio y el ViewModel pueden probarse con dobles (MockK).
- **Identidad visual:** el logo (`res/drawable/logo_app.xml`) y los colores (`ui/theme/Theme.kt` y `res/values/colors.xml`) son provisionales hasta contar con el manual de marca de la organización.
