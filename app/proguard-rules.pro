-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jnetaol.securemessenger.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
