plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.still"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.still"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-platform-proof"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

tasks.register("assertNoInternetPermission") {
    group = "verification"
    description = "Fails if the merged debug manifest declares INTERNET."
    dependsOn("processDebugMainManifest")
    doLast {
        val merged = layout.buildDirectory
            .file("intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml")
            .get()
            .asFile
        require(merged.isFile) { "Merged manifest missing at ${merged.absolutePath}" }
        val text = merged.readText()
        val internetPermission =
            Regex("""<uses-permission[^>]*android:name="android\.permission\.INTERNET" """)
        check(internetPermission.find(text) == null) {
            "Merged manifest must not declare android.permission.INTERNET:\n$text"
        }
    }
}

tasks.named("check") {
    dependsOn("assertNoInternetPermission")
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
