plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.e3hi.geodrop"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.e3hi.geodrop"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val visionApiKey = (project.findProperty("GOOGLE_VISION_API_KEY") as String?)
            ?: System.getenv("GOOGLE_VISION_API_KEY")
            ?: ""
        val escapedVisionKey = visionApiKey.replace("\"", "\\\"")
        buildConfigField("String", "GOOGLE_VISION_API_KEY", "\"$escapedVisionKey\"")

        val functionsRegion = (project.findProperty("FIREBASE_FUNCTIONS_REGION") as String?)
            ?: System.getenv("FIREBASE_FUNCTIONS_REGION")
            ?: "us-central1"
        val escapedFunctionsRegion = functionsRegion.replace("\"", "\\\"")
        buildConfigField(
            "String",
            "FIREBASE_FUNCTIONS_REGION",
            "\"$escapedFunctionsRegion\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    val composeVersion = "1.6.8"

    // Firebase BOM first (no versions on individual Firebase libs)
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))

    // Firebase Auth (plain, NON-KTX to avoid the unresolved ktx issue)
    implementation("com.google.firebase:firebase-auth")

    // Firestore & Messaging (KTX is fine; can be plain too if you prefer)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Play Services Location (for geofencing / fused location)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Google Maps Compose
    implementation("com.google.maps.android:maps-compose:4.3.3")

    // Compose + Material3
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.foundation:foundation:${composeVersion}")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.google.android.material:material:1.12.0")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
}
