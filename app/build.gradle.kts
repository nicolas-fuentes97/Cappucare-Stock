plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // KSP: genera el código de Room (DAO, base de datos) al compilar.
    alias(libs.plugins.ksp)
}

/**
 * Datos de firma del APK release. NO van en el repositorio: se leen (de forma opcional) desde
 * gradle.properties del usuario (~/.gradle/gradle.properties) o desde variables de entorno.
 * Si falta alguno, el build release se genera igual, pero SIN firmar.
 * Nombres: CAPPUCARE_STORE_FILE, CAPPUCARE_STORE_PASSWORD, CAPPUCARE_KEY_ALIAS, CAPPUCARE_KEY_PASSWORD.
 */
fun datoDeFirma(nombre: String): String? =
    (findProperty(nombre) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(nombre)?.takeIf { it.isNotBlank() }

val rutaAlmacenFirma = datoDeFirma("CAPPUCARE_STORE_FILE")
val claveAlmacenFirma = datoDeFirma("CAPPUCARE_STORE_PASSWORD")
val aliasFirma = datoDeFirma("CAPPUCARE_KEY_ALIAS")
val claveFirma = datoDeFirma("CAPPUCARE_KEY_PASSWORD")
val hayDatosDeFirma = listOf(rutaAlmacenFirma, claveAlmacenFirma, aliasFirma, claveFirma).all { it != null }

/**
 * Dirección inicial del servidor (API). Se lee de gradle.properties (API_BASE_URL) o de
 * -PAPI_BASE_URL=... en la línea de comandos; por defecto es el emulador de Android Studio
 * (10.0.2.2 = el PC que lo ejecuta). Siempre termina en "/" (requisito de Retrofit).
 */
val apiBaseUrl: String = ((findProperty("API_BASE_URL") as String?)?.trim()?.takeIf { it.isNotEmpty() }
    ?: "http://10.0.2.2:3000/")
    .replace("\"", "")
    .let { if (it.endsWith("/")) it else "$it/" }

/**
 * Bandera de la Experiencia 3: define SOLO el modo inicial de la app. false = modo local (solo
 * Room, sin servidor); true = modo servidor (la API, lo que implementes en los TODO(E3-n)). Se
 * cambia en gradle.properties (USAR_API=true) o con -PUSAR_API=true. Después, el usuario cambia el
 * modo desde la app (Login > Servidor > "Modo local") y esa preferencia queda guardada.
 */
val usarApi: Boolean = (findProperty("USAR_API") as String?)?.trim()?.equals("true", ignoreCase = true) ?: false

android {
    namespace = "com.cappucare.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cappucare.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // EA3: dirección inicial del servidor y bandera del modo API (ver arriba).
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "USAR_API", usarApi.toString())
    }

    signingConfigs {
        if (hayDatosDeFirma) {
            create("release") {
                storeFile = file(rutaAlmacenFirma!!)
                storePassword = claveAlmacenFirma
                keyAlias = aliasFirma
                keyPassword = claveFirma
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firma solo si hay datos de firma (ver arriba); si no, el APK release queda sin firmar.
            if (hayDatosDeFirma) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Navegación entre pantallas ---
    // navigation-compose 2.7.7 ya incluye el runtime de navegación. NO agregar
    // "navigation-runtime-android": esa coordenada no existe en la versión 2.7.7.
    implementation(libs.androidx.navigation.compose)

    // --- Íconos (flecha atrás, mostrar/ocultar clave, agregar, etc.) ---
    implementation(libs.androidx.material.icons.extended)

    // --- Persistencia local: Room (datos) + DataStore (último usuario) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // --- Recurso nativo: cámara para leer QR / código de barras (RNF-06) ---
    implementation(libs.zxing.android.embedded)

    // --- EA3: capa remota (API REST) con Retrofit + OkHttp (paquete data/remote/) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    // El registro HTTP solo existe en el build debug (ver src/debug/.../RegistroHttp.kt).
    debugImplementation(libs.okhttp.logging.interceptor)

    // --- Dependencias estándar del proyecto (template de Android Studio) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- Pruebas ---
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
