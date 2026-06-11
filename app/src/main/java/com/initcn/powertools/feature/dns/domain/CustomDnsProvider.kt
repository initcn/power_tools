package com.initcn.powertools.feature.dns.domain

data class CustomDnsProvider(
    val id: Long =
        System.currentTimeMillis(),

    val name: String,

    val hostname: String
)