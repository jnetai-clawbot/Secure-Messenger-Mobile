-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jnetaol.securemessenger.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
