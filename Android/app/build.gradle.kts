import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

val demoProperties = Properties().apply {
    rootProject.file("demo.properties").inputStream().use(::load)
}

fun demoProperty(name: String): String = demoProperties.getProperty(name)?.trim()
    ?.takeIf(String::isNotEmpty)
    ?: error("Set $name in demo.properties")

fun quoted(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.honeynotify.demo"
    compileSdk = 35

    defaultConfig {
        applicationId = demoProperty("application_id")
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "WEBVIEW_URL", quoted(demoProperty("webview_url")))
        buildConfigField(
            "String",
            "HONEYNOTIFY_API_URL",
            quoted(demoProperty("honeynotify_api_url"))
        )
        buildConfigField(
            "String",
            "HONEYNOTIFY_CLIENT_KEY",
            quoted(demoProperty("honeynotify_client_key"))
        )
        resValue("string", "app_name", demoProperty("app_name"))
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.honeynotify:android-sdk") {
        version {
            branch = "main"
        }
    }
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
}

configurations.configureEach {
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("app/google-services.json is missing; WebView builds, but FCM registration is disabled.")
}
