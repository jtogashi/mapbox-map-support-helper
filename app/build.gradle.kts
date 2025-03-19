plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "cc.jtogashi.mapboxmapsupporthelper"
    compileSdk = 35

    defaultConfig {
        applicationId = "cc.jtogashi.mapboxmapsupporthelper"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        resValue(
            "string",
            "mapbox_access_token",
            providers.gradleProperty("MAPBOX_PUBLIC_TOKEN").get()
        )

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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work)

    implementation(libs.kotlinx.coroutine)

    implementation(libs.gms.location)

    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.java.turf)
    implementation(libs.mapbox.java.services)

    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}