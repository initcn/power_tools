package com.initcn.powertools.feature.callblocker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.initcn.powertools.feature.callblocker.domain.RuleType
import java.util.UUID

@Entity(tableName = "call_rules")
data class CallRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "pattern")
    val pattern: String,

    @ColumnInfo(name = "ruleType")
    val ruleType: RuleType,

    @ColumnInfo(name = "label")
    val label: String? = null,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)