# Proguard rules for Alice Home Widget
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.alice.homewidget.model.** { *; }
