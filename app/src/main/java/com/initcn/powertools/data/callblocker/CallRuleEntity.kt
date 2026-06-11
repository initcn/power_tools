package com.initcn.powertools.data.callblocker

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "call_rules")
data class CallRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "pattern")
    val pattern: String, // The phone number or regex pattern

    @ColumnInfo(name = "ruleType")
    val ruleType: RuleType,

    @ColumnInfo(name = "label")
    val label: String? = null, // e.g., "Mom", "Spam Prefix", "UK Numbers"

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)