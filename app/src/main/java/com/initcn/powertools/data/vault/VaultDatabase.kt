package com.initcn.powertools.data.vault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.initcn.powertools.crypto.VaultDbKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// Reset to Version 1. A perfectly clean slate.
@Database(entities = [VaultFileEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = VaultDbKeyManager.getDatabasePassphrase(context)
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_metadata_db"
                )
                    .openHelperFactory(factory)
                    // FIX: Explicitly tell Room to drop everything on migration
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}