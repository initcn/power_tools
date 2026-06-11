@file:Suppress("UnstableApiUsage")

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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()

        // FIXED: Open up the broad mavenCentral layout engine
        // so standard dependencies (Kotlin, Coroutines, Gson, JUnit) can be fetched
        mavenCentral()

        // Fallback option in case specific library submodules need dedicated resolution mirrors
        maven { url = java.net.URI("https://plugins.gradle.org/m2/") }
    }
}

rootProject.name = "Power Tools"
include(":app")