import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.secrets)
}

// Versionsnummer: In GitHub Actions wird die Laufnummer übergeben (-PversionCode=…),
// damit jede neue APK als Update über die alte installiert werden kann.
val appVersionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

// Signierdaten kommen aus local.properties ODER aus Umgebungsvariablen – nie aus dem Quellcode.
// In GitHub Actions setzt der Workflow SIGNING_KEYSTORE_PATH / SIGNING_PASSWORD aus den Repository-Secrets.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String): String? =
    localProps.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

val signingStoreFile = secret("RELEASE_STORE_FILE") ?: secret("SIGNING_KEYSTORE_PATH")
val signingStorePassword = secret("RELEASE_STORE_PASSWORD") ?: secret("SIGNING_PASSWORD")
val signingKeyPassword = secret("RELEASE_KEY_PASSWORD") ?: secret("SIGNING_PASSWORD")
val signingKeyAlias = secret("RELEASE_KEY_ALIAS") ?: secret("SIGNING_KEY_ALIAS") ?: "rezeptbuch"
val canSign = signingStoreFile != null && signingStorePassword != null && signingKeyPassword != null

android {
    namespace = "de.rezeptbuch.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.rezeptbuch.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "1.0.$appVersionCode"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Ohne konfigurierten Keystore bleibt der Release-Build unsigniert.
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

secrets {
    // Echte Werte: local.properties (git-ignoriert). Fallback: committete Platzhalter.
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
    ignoreList.add("sdk.dir")
    ignoreList.add("RELEASE_.*")
    ignoreList.add("SIGNING_.*")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
}
