# ProGuard rules for JARVIS
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jarvis.app.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
