package com.initcn.powertools.core.di

import android.content.Context
import android.content.SharedPreferences
import com.initcn.powertools.feature.callblocker.data.CallBlockerDatabase
import com.initcn.powertools.feature.callblocker.data.CallRuleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    // 1. Provide SharedPreferences globally
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("powertools_preferences", Context.MODE_PRIVATE)
    }

    // 2. Provide the Call Blocker Database
    @Provides
    @Singleton
    fun provideCallBlockerDatabase(@ApplicationContext context: Context): CallBlockerDatabase {
        return CallBlockerDatabase.getDatabase(context)
    }

    // 3. Provide the DAO directly (so ViewModels don't need to know about the Database)
    @Provides
    @Singleton
    fun provideCallRuleDao(database: CallBlockerDatabase): CallRuleDao {
        return database.callRuleDao()
    }
}