plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kummer.workstatus"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kummer.workstatus"
        minSdk = 29
        targetSdk = 34
        versionCode = 15
        versionName = "1.15"

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

    packagingOptions {
        resources {
            pickFirsts.add("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.log4j12)
    implementation("de.mindpipe.android:android-logging-log4j:1.0.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}