# ==========================================
# PowerTools Release ProGuard / R8 Rules
# ==========================================

# Remove logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Kotlin Metadata
-keep class kotlin.Metadata { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==========================================
# CRITICAL APP DEPENDENCIES (SCOPED)
# ==========================================

# 1. SQLCipher (Target only the JNI native bindings to prevent crashes)
-keepclasseswithmembernames class net.sqlcipher.** { native <methods>; }
-keepclasseswithmembernames class net.zetetic.database.sqlcipher.** { native <methods>; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.database.sqlcipher.**

# 2. Gson Data Models (Explicitly target ONLY the model saved to JSON)
-keep class com.initcn.powertools.model.CustomDnsProvider { *; }

# 3. Room Database Entities (Explicitly target ONLY the DB entity)
-keep class com.initcn.powertools.data.vault.VaultFileEntity { *; }

# ==========================================
# Suppress Warnings for Safe Libraries
# ==========================================
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.**