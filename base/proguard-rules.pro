# ProGuard Configuration file
#
# See http://proguard.sourceforge.net/index.html#manual/usage.html

# Crashlytics Deobfuscation
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.
