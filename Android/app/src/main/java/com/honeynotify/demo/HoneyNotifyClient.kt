package com.honeynotify.demo

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.honeynotify.HoneyNotify

object HoneyNotifyClient {
    private const val LOG_TAG = "HoneyNotifyDemo"

    @Volatile
    private var client: HoneyNotify? = null

    fun get(context: Context): HoneyNotify {
        return client ?: synchronized(this) {
            client ?: HoneyNotify(
                context.applicationContext,
                BuildConfig.HONEYNOTIFY_API_URL,
                BuildConfig.HONEYNOTIFY_CLIENT_KEY
            ).also { client = it }
        }
    }

    fun register(context: Context) {
        if (!BuildConfig.HONEYNOTIFY_CLIENT_KEY.startsWith("ps_public_") ||
            BuildConfig.HONEYNOTIFY_CLIENT_KEY == "ps_public_replace_me"
        ) {
            Log.w(LOG_TAG, "Set a ps_public_ honeynotify_client_key in demo.properties")
            return
        }

        if (FirebaseApp.initializeApp(context) == null) {
            Log.w(LOG_TAG, "Firebase is not configured; add app/google-services.json")
            return
        }

        runCatching {
            get(context).registerCurrentToken(tags = mapOf("source" to "webview-demo")) { result ->
                result.onSuccess { deviceId ->
                    Log.i(LOG_TAG, "Registered HoneyNotify device $deviceId")
                }.onFailure { error ->
                    Log.e(LOG_TAG, "HoneyNotify registration failed", error)
                }
            }
        }.onFailure { error ->
            Log.e(LOG_TAG, "Unable to start Firebase registration", error)
        }
    }
}
