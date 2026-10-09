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
