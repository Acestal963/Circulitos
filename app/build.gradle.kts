plugins {
<<<<<<< HEAD
    alias(libs.plugins.android.application)  // Solo una vez (eliminé el duplicado)
    alias(libs.plugins.google.services)     // Plugin de Google Services
}

android {
    namespace = "com.olivo_de_leon_osiel_alejandro.proyecto_equipo_u2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.olivo_de_leon_osiel_alejandro.proyecto_equipo_u2"
=======
    alias(libs.plugins.android.application)
}

android {
    namespace = "upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02"
    compileSdk = 35

    defaultConfig {
        applicationId = "upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02"
>>>>>>> e41488bb97eeb0f8d9d48eff7f6045e35850b2fc
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
<<<<<<< HEAD
    // AndroidX
=======

>>>>>>> e41488bb97eeb0f8d9d48eff7f6045e35850b2fc
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
<<<<<<< HEAD

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase (usando BOM)
    implementation(platform(libs.firebase.bom))  // Usando la referencia del catalogo
    implementation("com.google.firebase:firebase-analytics-ktx")  // Nota el -ktx
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.0")
=======
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
>>>>>>> e41488bb97eeb0f8d9d48eff7f6045e35850b2fc
}