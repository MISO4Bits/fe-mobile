pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Resuelve y descarga el JDK del toolchain cuando la maquina no lo tiene.
// Evita que cada integrante tenga que instalar la misma version a mano.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // El mapa de las oficinas (BITS-368). La descarga no pide token.
        maven("https://api.mapbox.com/downloads/v2/releases/maven") {
            content { includeGroupByRegex("com\\.mapbox.*") }
        }
    }
}

rootProject.name = "fe-mobile"
include(":app")
