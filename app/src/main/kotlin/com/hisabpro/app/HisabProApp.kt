package com.hisabpro.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.DatabaseMigrationHelper
import com.hisabpro.app.ads.AdsManager

class HisabProApp : Application() {

    companion object {
        private var activeActivityRef: java.lang.ref.WeakReference<Activity>? = null

        val currentActivity: Activity?
            get() = activeActivityRef?.get()
    }

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activeActivityRef = java.lang.ref.WeakReference(activity)
            }
            override fun onActivityStarted(activity: Activity) {
                activeActivityRef = java.lang.ref.WeakReference(activity)
            }
            override fun onActivityResumed(activity: Activity) {
                activeActivityRef = java.lang.ref.WeakReference(activity)
            }
            override fun onActivityPaused(activity: Activity) {
                if (activeActivityRef?.get() == activity) {
                    activeActivityRef = null
                }
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activeActivityRef?.get() == activity) {
                    activeActivityRef = null
                }
            }
        })

        val db = AppDatabase.getInstance(this)
        DatabaseMigrationHelper.migrateIfNecessary(this, db)
        clearDemoPrefs()
        // MobileAds initialization is intentionally deferred to MainActivity after UMP consent resolution
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
