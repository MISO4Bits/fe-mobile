import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
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
