package com.hisabpro.app

import android.app.Application
import android.content.Context
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.DatabaseMigrationHelper

class HisabProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        DatabaseMigrationHelper.migrateIfNecessary(this, db)
        clearDemoPrefs()
    }

    private fun clearDemoPrefs() {
        try {
            val sharedPrefsDir = java.io.File(applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                sharedPrefsDir.listFiles()?.forEach { file ->
                    val name = file.name
                    if (name.contains("hisab_pro_purchases") || 
                        name.contains("hisab_pro_invoices") || 
                        name.contains("hisab_pro_items") || 
                        name.contains("hisab_pro_transactions") ||
                        name.contains("hisab_pro_parties")) {
                        val prefsName = name.removeSuffix(".xml")
                        getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().apply()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
