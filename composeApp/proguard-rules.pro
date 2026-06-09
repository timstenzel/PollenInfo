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
