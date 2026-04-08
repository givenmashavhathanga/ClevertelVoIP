# MizuVoIP SDK - keep all public API
-keep class com.mizuvoip.jvoip.** { *; }
-keep class com.mizuvoip.mizudroid.** { *; }

# Keep app entry points
-keep class co.za.clevertel.voip.** { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**
