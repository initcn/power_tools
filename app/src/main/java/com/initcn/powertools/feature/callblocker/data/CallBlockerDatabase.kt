package com.initcn.powertools.feature.callblocker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallRuleEntity::class], version = 1, exportSchema = false)
abstract class CallBlockerDatabase : RoomDatabase() {

    abstract fun callRuleDao(): CallRuleDao

    companion object {
        @Volatile
        private var INSTANCE: CallBlockerDatabase? = null

        fun getDatabase(context: Context): CallBlockerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CallBlockerDatabase::class.java,
                    "call_blocker_db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}