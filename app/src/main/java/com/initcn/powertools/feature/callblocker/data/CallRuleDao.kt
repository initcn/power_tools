package com.initcn.powertools.feature.callblocker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.initcn.powertools.feature.callblocker.domain.RuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRuleDao {

    // --- UI Queries (Reactive Streams for Compose) ---

    @Query("SELECT * FROM call_rules ORDER BY createdAt DESC")
    fun getAllRulesFlow(): Flow<List<CallRuleEntity>>

    @Query("SELECT * FROM call_rules WHERE ruleType = :type ORDER BY createdAt DESC")
    fun getRulesByTypeFlow(type: RuleType): Flow<List<CallRuleEntity>>

    // --- Engine Queries (Fast, one-shot reads for the CallScreeningService) ---

    @Query("SELECT * FROM call_rules WHERE ruleType = 'WHITELIST'")
    suspend fun getWhitelistSync(): List<CallRuleEntity>

    // Used for exporting all block rules at once in the ViewModel
    @Query("SELECT * FROM call_rules WHERE ruleType != 'WHITELIST'")
    suspend fun getBlocklistSync(): List<CallRuleEntity>

    // Used by the background service for precise exact-number matching
    @Query("SELECT * FROM call_rules WHERE ruleType = 'BLOCKLIST_EXACT'")
    suspend fun getExactBlocklistSync(): List<CallRuleEntity>

    // Used by the background service for regex pattern matching
    @Query("SELECT * FROM call_rules WHERE ruleType = 'BLOCKLIST_REGEX'")
    suspend fun getRegexBlocklistSync(): List<CallRuleEntity>

    // --- Mutations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CallRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CallRuleEntity>) // For batch importing

    @Delete
    suspend fun deleteRule(rule: CallRuleEntity)

    @Query("DELETE FROM call_rules")
    suspend fun deleteAllRules() // For factory resets / massive imports
}