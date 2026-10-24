import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing comes from environment variables (set by CI from repository secrets).
// Without them the release build falls back to the debug key so it still installs.
// `-Pdemo` builds app.fogwalk.demo with a made-up explored city for screenshots, next to the real install.
val demo = project.hasProperty("demo")

val releaseKeystore = System.getenv("SIGNING_KEYSTORE_PATH")?.let(::file)?.takeIf { it.exists() }

android {
    namespace = "app.fogwalk"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.fogwalk"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        buildConfigField("boolean", "DEMO", demo.toString())
        if (demo) applicationIdSuffix = ".demo"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Keeps a debug install from ever replacing (or wiping) the real app on the same phone.
            if (!demo) applicationIdSuffix = ".dev"
            if (project.hasProperty("r8")) {
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    // Demo walks for screenshots ride along only in -Pdemo builds.
    if (demo) sourceSets["main"].assets.srcDir("src/demo/assets")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    // Play otherwise installs only the device's languages from the AAB,
    // and the in-app switch to Russian would find no Russian strings.
    bundle {
        language {
            enableSplit = false
        }
    }
    lint {
        error += "MissingTranslation"
        // Dependency versions are pinned on purpose (AGP 8.13 + Kotlin 2.2 toolchain).
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        // Adaptive icons stay in mipmap-anydpi-v26: aapt does not resolve them from plain mipmap-anydpi.
        disable += "ObsoleteSdkInt"
        warningsAsErrors = false
        abortOnError = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.osmdroid)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
