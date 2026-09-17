pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HoneyNotifyWebViewDemo"
include(":app")

sourceControl {
    gitRepository(uri("https://github.com/HoneyNotify/android-SDK.git")) {
        producesModule("com.honeynotify:android-sdk")
    }
}
