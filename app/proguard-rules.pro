# LibretroDroid calls back into Java from JNI (rumble, errors) and its views rely on
# reflection-based lifecycle callbacks, so its classes must survive shrinking untouched.
-keep class com.swordfish.libretrodroid.** { *; }
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent *;
}
