package com.initcn.powertools.feature.callblocker.domain

data class CallLogEntry(
    val number: String,
    val name: String?,
    val date: Long,
    val type: Int // Incoming, Outgoing, Missed, Blocked
)