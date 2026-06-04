# Remove logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Kotlin
-keep class kotlin.Metadata { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Navigation
-keep class androidx.navigation.** { *; }

-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.**