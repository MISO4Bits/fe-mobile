plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

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

        // Por ahora el movil habla con el BFF Web: bff-mobile aun no tiene
        // contrato. Retrofit exige que la URL base termine en "/".
        buildConfigField("String", "URL_BASE_BFF", "\"https://dev.solventa4bits.com/web/\"")
    }

    buildTypes {
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
        getByName("test") { kotlin.srcDir("src/test/kotlin") }
        getByName("androidTest") { kotlin.srcDir("src/androidTest/kotlin") }
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

    // Estado de pantalla y navegacion entre pantallas.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

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
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
