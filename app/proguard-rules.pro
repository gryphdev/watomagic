# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room uses these annotations for its processing logic. Keep them to avoid issues.
-keep class androidx.room.RoomDatabase { *; }
-keep class androidx.room.RoomSQLiteQuery { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature

# Gson
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Moshi
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep class **JsonAdapter { *; }

# Rhino JavaScript Engine
# Rhino includes debug tools (org.mozilla.javascript.tools) that depend on
# AWT/Swing classes not available on Android. These tools are not used in
# the Android runtime. The debug tools are never instantiated in our code,
# so we can safely ignore the missing class references.

# Ignore missing AWT/Swing classes referenced by Rhino debug tools
# These are only used by desktop debug tools, not needed for Android runtime
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.**
-dontwarn org.mozilla.javascript.tools.**

# R8 treats missing classes as errors, not warnings. Since the debug tools
# that reference AWT/Swing are never used in Android, we allow R8 to ignore
# these missing class errors. This is safe because:
# 1. The tools package is never instantiated in our code
# 2. Only the core Rhino engine is used (Context, Scriptable, etc.)
# 3. The missing classes are only referenced by unused debug tools
-ignorewarnings

# Keep Rhino core classes that are actually used by BotJS
# Note: We don't keep the tools package as it's not used and causes AWT/Swing dependencies
-keep class org.mozilla.javascript.Context { *; }
-keep class org.mozilla.javascript.Scriptable { *; }
-keep class org.mozilla.javascript.ScriptableObject { *; }
-keep class org.mozilla.javascript.Function { *; }
-keep class org.mozilla.javascript.NativeObject { *; }
-keep class org.mozilla.javascript.NativeJSON { *; }
-keep class org.mozilla.javascript.ImporterTopLevel { *; }
-keep class org.mozilla.javascript.JavaToJSONConverters { *; }
-keepclassmembers class org.mozilla.javascript.** { *; }
