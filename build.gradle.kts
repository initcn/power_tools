plugins {
    // Declares the plugins on the global classpath without executing them at the root level
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.com.google.devtools.ksp) apply false
}