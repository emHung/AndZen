import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val appLocalProperties = Properties().also { properties ->
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { properties.load(it) }
}
val releaseApiUrl = appLocalProperties.getProperty("sanbong.apiBaseUrl", "")
val releaseSupabaseUrl = appLocalProperties.getProperty("sanbong.supabaseUrl", "")
fun buildConfigValue(key: String, fallback: String = ""): String =
    "\"${(appLocalProperties.getProperty(key, fallback)).replace("\\", "\\\\").replace("\"", "\\\"")}\""

val verifyReleaseEndpoints = tasks.register("verifyReleaseEndpoints") {
    doLast {
        if (releaseApiUrl.isBlank() || !releaseApiUrl.startsWith("https://", ignoreCase = true)) {
            throw GradleException("Bản release yêu cầu sanbong.apiBaseUrl được cấu hình bằng HTTPS.")
        }
        if (releaseSupabaseUrl.isBlank() || !releaseSupabaseUrl.startsWith("https://", ignoreCase = true)) {
            throw GradleException("Bản release yêu cầu sanbong.supabaseUrl được cấu hình bằng HTTPS.")
        }
    }
}

tasks.configureEach {
    if (name != "verifyReleaseEndpoints" && name.contains("Release", ignoreCase = true)) dependsOn(verifyReleaseEndpoints)
}

android {
    namespace = "com.sanbong.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sanbong.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", buildConfigValue("sanbong.supabaseUrl"))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", buildConfigValue("sanbong.supabasePublishableKey"))
        buildConfigField("String", "API_BASE_URL", buildConfigValue("sanbong.apiBaseUrl", if (releaseApiUrl.isBlank()) "http://127.0.0.1:8000" else releaseApiUrl))
        buildConfigField("boolean", "PHONE_OTP_ENABLED", appLocalProperties.getProperty("sanbong.phoneOtpEnabled", "false").toBoolean().toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    testImplementation(libs.junit)
}
