plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

    // Reglas de negocio: JUnit 5, como declara la Estrategia de Pruebas.
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Interfaz instrumentada: Espresso y las pruebas de Compose.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
