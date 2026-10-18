# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }

# Keep data classes used in serialization
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Koin
-keep class org.koin.** { *; }

# Ktor's IntelliJ debugger detector (io.ktor.util.debug) reads JVM management beans that Android
# does not have. The code path never runs on Android; without these R8 fails on the missing classes.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Crashlytics: readable stack traces. The uploaded mapping restores class and method names; these
# keep the source file and line numbers it needs, and keep custom exception types unrenamed so
# Crashlytics groups crashes by their real type. The library's own consumer rules do not cover them.
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
