package com.initcn.powertools.feature.vault.di

import android.content.Context
import com.initcn.powertools.feature.vault.data.VaultDao
import com.initcn.powertools.feature.vault.data.VaultDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VaultModule {

    @Provides
    @Singleton
    fun provideVaultDatabase(@ApplicationContext context: Context): VaultDatabase {
        return VaultDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideVaultDao(database: VaultDatabase): VaultDao {
        return database.vaultDao()
    }
}