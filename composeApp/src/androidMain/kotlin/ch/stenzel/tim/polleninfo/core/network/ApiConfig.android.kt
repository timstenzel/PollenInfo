package ch.stenzel.tim.polleninfo.core.network

/**
 * `10.0.2.2` is the Android emulator's alias for the host machine's loopback interface, so this
 * reaches `./gradlew :server:run` running on the development machine. Cleartext is permitted for
 * debug builds only (`src/debug/AndroidManifest.xml`); replace this with an `https` address once
 * the backend is deployed.
 */
actual val apiBaseUrl: String = "http://10.0.2.2:8080"
