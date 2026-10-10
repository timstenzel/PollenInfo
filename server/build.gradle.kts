plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ch.stenzel.tim.polleninfo.server.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // The SLF4J backend. Without one every log line — the scheduler's, the push fallback's, call
    // logging — is silently dropped.
    runtimeOnly(libs.logback.classic)

    // Outbound leg to the MeteoSwiss OGD file service. CIO because the server has no other engine
    // requirement and it pulls in no platform HTTP stack.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    // Alarms and device registrations survive a restart: PostgreSQL through Exposed's DSL, the
    // schema from Flyway's versioned migrations, the connections from a small Hikari pool.
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // Push: the FCM HTTP v1 call goes through the Ktor client above; this library only turns the
    // service-account key into an access token.
    implementation(libs.google.auth.oauth2)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    // The database tests' PostgreSQL, the production version, in Docker.
    testImplementation(libs.testcontainers.postgresql)
}
