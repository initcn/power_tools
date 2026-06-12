# ==========================================
# PowerTools Release ProGuard / R8 Rules
# ==========================================

# --- 1. General Optimizations ---
# Strip debug and verbose logs from the release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep standard Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Enums safe from aggressive renaming
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- 2. SQLCipher / Database Native Bindings ---
# CRITICAL: Prevent R8 from obfuscating JNI bridges to avoid vault crashes
-keepclasseswithmembernames class net.sqlcipher.** { native <methods>; }
-keepclasseswithmembernames class net.zetetic.database.sqlcipher.** { native <methods>; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.database.sqlcipher.**

# --- 3. Gson Serialization & Room Entities ---
# Explicitly keep data classes used for JSON serialization and Room reflections
-keep class com.initcn.powertools.feature.dns.domain.CustomDnsProvider { *; }
-keep class com.initcn.powertools.feature.vault.data.VaultFileEntity { *; }
-keep class com.initcn.powertools.feature.callblocker.data.CallRuleEntity { *; }

# --- 4. Library Warnings Suppression ---
# Suppress harmless warnings from standard Google/AndroidX libraries
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.**
-dontwarn com.google.errorprone.annotations.**