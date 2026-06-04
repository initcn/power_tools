package com.initcn.powertools.model

data class CustomDnsProvider(
    val id: Long =
        System.currentTimeMillis(),

    val name: String,

    val hostname: String
)