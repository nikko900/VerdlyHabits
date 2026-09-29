# Verdly Habits — release shrink rules (minify currently disabled).

-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
