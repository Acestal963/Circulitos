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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
<<<<<<< HEAD


}

rootProject.name = "Proyecto_Equipo_U2"
=======
}

rootProject.name = "Z_U_2_iti-271415_E_02"
>>>>>>> e41488bb97eeb0f8d9d48eff7f6045e35850b2fc
include(":app")
 