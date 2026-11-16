plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.akkhara.keyboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.akkhara.keyboard"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
    }
}

dependencies {
    implementation(project(":engine"))
}
