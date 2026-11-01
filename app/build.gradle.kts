import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
    // Avisos push (BITS-229): lee app/google-services.json, que sale de iac-gcp-dev/persistent.
    alias(libs.plugins.google.services)
}

// URL del BFF en la version de depuracion. Por defecto es el BFF levantado en
// el computador de quien desarrolla (desde el emulador, 10.0.2.2 es el
// localhost de ese computador). Para probar contra otro ambiente se define
// bff.url en local.properties, que no se versiona:
//   bff.url=https://dev.solventa4bits.com/mobile/
val urlBffDeDepuracion: String = Properties()
    .apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }
    .getProperty("bff.url", "http://10.0.2.2:8082/")

// Cobertura de pruebas automatizadas. El umbral es el 80 % que declara la
// Estrategia de Pruebas y al que apunta el criterio CP-4 de cada historia.
//
// Se mide sobre lo que las pruebas unitarias pueden cubrir: reglas de
// negocio. La interfaz de Compose se verifica con Espresso en pruebas
// instrumentadas, que corren en un emulador y no entran en este conteo, asi
// que queda fuera del calculo en vez de hundir el porcentaje.
kover {
    reports {
        filters {
            excludes {
                // Punto de entrada de Android y arboles de interfaz.
                classes(
                    "com.solventa4bits.movil.MainActivity*",
                    "com.solventa4bits.movil.MainActivityKt*",
                    "com.solventa4bits.movil.ui.tema.TemaKt",
                )
                // Depende del Keystore del dispositivo: se verifica con una
                // prueba instrumentada (BovedaKeystoreTest).
                classes("com.solventa4bits.movil.sesion.datos.BovedaKeystore*")
                // Pregunta al sistema por la red: depende del dispositivo.
                classes("com.solventa4bits.movil.red.ConexionDelTelefono*")
                // La base de datos y el codigo que Room genera: se verifican con
                // una prueba instrumentada (PolizasDaoTest).
                classes("com.solventa4bits.movil.seguro.datos.BaseDeDatosSolventa*", "*_Impl*")
                // El dialogo biometrico del sistema no se automatiza; las
                // preferencias del dispositivo se verifican con una prueba
                // instrumentada (PreferenciasDelTelefonoTest).
                classes(
                    "com.solventa4bits.movil.sesion.datos.AutenticadorBiometrico*",
                    "com.solventa4bits.movil.sesion.datos.PreferenciasDelTelefono*",
                )
                // Piezas de los avisos que dependen de Android o de Firebase: el servicio de
                // mensajeria, la Application, el permiso del sistema y el token. La logica
                // (registro, destino, permiso pedido una vez) esta en avisos.dominio y si se mide.
                classes(
                    "com.solventa4bits.movil.AplicacionSolventa*",
                    "com.solventa4bits.movil.avisos.ServicioDeAvisos*",
                    "com.solventa4bits.movil.avisos.datos.FuenteDeTokenFirebase*",
                    "com.solventa4bits.movil.avisos.datos.PermisoDelTelefono*",
                )
                // Usa imagenes reales de Android: se verifica con una prueba
                // instrumentada (PreparadorDeFotosTest).
                classes("com.solventa4bits.movil.identidad.datos.PreparadorDeFotos*")
                // Clases que genera el compilador de Compose.
                classes("*ComposableSingletons*", "*_Factory*", "*Hilt*")
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        verify {
            rule("Cobertura minima del 80 %") {
                bound { minValue.set(80) }
            }
        }
    }
}

android {
    namespace = "com.solventa4bits.movil"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.solventa4bits.movil"
        // Android 8. Es el piso que sostiene biometria, camara, notificaciones
        // y ubicacion con las APIs modernas, sin arrastrar compatibilidad vieja.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("es")

        // URL del BFF del canal movil. Retrofit exige que termine en "/".
        buildConfigField("String", "URL_BASE_BFF", "\"https://dev.solventa4bits.com/mobile/\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "URL_BASE_BFF", "\"$urlBffDeDepuracion\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") { kotlin.srcDir("src/main/kotlin") }
        // sharedTest: el juego de datos de prueba, comun a unitarias e instrumentadas (BITS-280).
        getByName("test") { kotlin.srcDirs("src/test/kotlin", "src/sharedTest/kotlin") }
        getByName("androidTest") { kotlin.srcDirs("src/androidTest/kotlin", "src/sharedTest/kotlin") }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.useJUnitPlatform() }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    // Estado de pantalla.
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Red: Retrofit sobre OkHttp, con JSON de kotlinx.serialization.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)

    // Camara de la cedula (BITS-275): CameraX, la libreria oficial de Android.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Huella (BITS-353): el dialogo biometrico del sistema.
    implementation(libs.androidx.biometric)
    // biometric trae fragment 1.2.5, que no acepta el permiso de avisos que pide Compose (BITS-229).
    implementation(libs.androidx.fragment)

    // Poliza sin conexion (BITS-217): Room guarda en el telefono lo ultimo que llego.
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Avisos push (BITS-229): Firebase Cloud Messaging. El BOM fija las versiones.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Reglas de negocio: JUnit 5, como declara la Estrategia de Pruebas.
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Dobles de prueba para la red y control de corrutinas.
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)

    // Interfaz instrumentada: Espresso y las pruebas de Compose.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    // BFF falso para la prueba de extremo a extremo.
    androidTestImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
