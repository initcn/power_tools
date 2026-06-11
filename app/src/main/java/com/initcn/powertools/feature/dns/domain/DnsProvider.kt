package com.initcn.powertools.feature.dns.domain

enum class DnsProvider(
    val title: String,
    val hostname: String?
) {

    OFF(
        title = "Off",
        hostname = null
    ),

    AUTOMATIC(
        title = "Automatic",
        hostname = null
    ),

    CLOUDFLARE(
        title = "Cloudflare",
        hostname = "one.one.one.one"
    ),

    GOOGLE(
        title = "Google",
        hostname = "dns.google"
    ),

    ADGUARD(
        title = "AdGuard",
        hostname = "dns.adguard.com"
    ),

    QUAD9(
        title = "Quad9",
        hostname = "dns.quad9.net"
    ),

    NEXTDNS(
        title = "NextDNS",
        hostname = "dns.nextdns.io"
    ),

    CLEANBROWSING(
        title = "CleanBrowsing",
        hostname = "security-filter-dns.cleanbrowsing.org"
    ),

    OPENDNS(
        title = "OpenDNS",
        hostname = "dns.opendns.com"
    ),

    CUSTOM(
        title = "Custom",
        hostname = null
    );

    companion object {

        val BuiltInProviders: List<DnsProvider>
            get() = entries.filter {
                it != OFF &&
                        it != AUTOMATIC &&
                        it != CUSTOM
            }
    }
}