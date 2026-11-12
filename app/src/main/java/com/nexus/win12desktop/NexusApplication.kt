package com.nexus.win12desktop

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class NexusApplication : Application() {

    companion object {
        lateinit var instance: NexusApplication
            private set
        
        fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences("nexus_win12_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
