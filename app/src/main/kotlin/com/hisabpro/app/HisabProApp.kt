package com.hisabpro.app

import android.app.Application
import com.hisabpro.app.data.local.AppDatabase
import com.hisabpro.app.data.local.DatabaseMigrationHelper

class HisabProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        DatabaseMigrationHelper.migrateIfNecessary(this, db)
    }
}
