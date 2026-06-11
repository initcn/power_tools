# ==========================================
# PowerTools Release ProGuard / R8 Rules
# ==========================================

# --- 1. General Optimizations ---
# Strip all debug and verbose logs from the release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep standard Kotlin metadata (often required by serialization libraries)
-keep class kotlin.Metadata { *; }

# Keep Enums safe from aggressive renaming (standard safety rule)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- 2. SQLCipher / Database Native Bindings ---
# CRITICAL: Prevent R8 from obfuscating the JNI bridges, or the encrypted vault will instantly crash
-keepclasseswithmembernames class net.sqlcipher.** { native <methods>; }
-keepclasseswithmembernames class net.zetetic.database.sqlcipher.** { native <methods>; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.database.sqlcipher.**

# --- 3. Gson Serialization & Room Entities ---
# Explicitly keep classes that are converted to/from JSON or used directly by Room Reflections.
# CustomDnsProvider is serialized to SharedPreferences via AppPreferences
-keep class com.initcn.powertools.feature.dns.domain.CustomDnsProvider { *; }

# VaultFileEntity is queried directly via Room DAO reflections
-keep class com.initcn.powertools.feature.vault.data.VaultFileEntity { *; }

# CallRuleEntity is exported/imported to JSON files in CallBlockerViewModel AND used by Room
-keep class com.initcn.powertools.feature.callblocker.data.CallRuleEntity { *; }

# --- 4. Library Warnings Suppression ---
# Suppress harmless warnings from standard Google/AndroidX libraries
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.**
-dontwarn com.google.errorprone.annotations.**