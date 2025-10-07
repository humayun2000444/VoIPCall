package com.voipcall

import android.app.Application
import android.util.Log

class VoIPCallApplication : Application() {

    companion object {
        private const val TAG = "VoIPCallApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
    }
}
