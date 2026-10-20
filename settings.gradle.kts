pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LibretroDroid (the libretro frontend library) is published through JitPack.
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\.Swordfish90.*") }
        }
    }
}

rootProject.name = "RetroHall"
include(":app", ":core")
