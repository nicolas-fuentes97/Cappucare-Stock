plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // KSP: genera el código de Room (DAO, base de datos) al compilar.
    alias(libs.plugins.ksp)
    // JaCoCo (viene con Gradle): mide qué parte del código recorren las pruebas unitarias.
    jacoco
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
        debug {
            // Activa la medición de cobertura en las pruebas unitarias (la usa la tarea reporteCobertura).
            enableUnitTestCoverage = true
        }
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

    testOptions {
        // Las pruebas JVM no tienen Android: las llamadas a clases de Android devuelven valores por defecto.
        unitTests.isReturnDefaultValues = true
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

    // --- API externa (mindicador.cl) con Retrofit + Gson, como en la Guía 14 (paquete data/remote/) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

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

/**
 * Reporte de cobertura de la LÓGICA de la app (ViewModel, repositorio y reglas de negocio).
 * Ejecuta las pruebas unitarias y deja el informe en app/build/reports/jacoco/reporteCobertura/
 * (html/index.html para abrir en el navegador y reporteCobertura.xml).
 * Se excluyen las pantallas (Compose), la base Room y los modelos de datos: no tienen lógica propia.
 */
tasks.register<JacocoReport>("reporteCobertura") {
    group = "verification"
    description = "Pruebas unitarias + cobertura de viewmodel, repositorio y reglas"
    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    val incluidas = listOf(
        "com/cappucare/app/viewmodel/**",
        "com/cappucare/app/data/repository/**",
        "com/cappucare/app/reglas/**"
    )
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { include(incluidas) }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    )
}

// Al correr las pruebas unitarias (configuración "Pruebas unitarias") se genera también el reporte de cobertura.
tasks.withType<Test>().configureEach {
    if (name == "testDebugUnitTest") finalizedBy("reporteCobertura")
}
