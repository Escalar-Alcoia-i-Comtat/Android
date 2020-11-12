# ProGuard Configuration file
#
# See http://proguard.sourceforge.net/index.html#manual/usage.html

# Fix maps 3.0.0-beta crash:
-keep,allowoptimization class com.google.android.libraries.maps.** { *; }

# Fix maps 3.0.0-beta marker taps ignored:
-keep,allowoptimization class com.google.android.apps.gmm.renderer.** { *; }