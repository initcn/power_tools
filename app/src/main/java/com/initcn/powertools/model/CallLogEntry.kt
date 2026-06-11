package com.initcn.powertools.model

data class CallLogEntry(
    val number: String,
    val name: String?,
    val date: Long,
    val type: Int // Incoming, Outgoing, Missed, Blocked
)