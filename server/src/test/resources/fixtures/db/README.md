# `polleninfo-exposed-0.61.db`

A server database written by the server **before** the Exposed 1.x migration, opened by
`LegacyDatabaseCompatibilityTest` to prove that an existing production file still loads.

- **Produced on:** Exposed 0.61.0, sqlite-jdbc as in `libs.versions.toml` at commit 4d13163,
  Kotlin 2.1.21, on 2026-10-09.
- **Do not regenerate it** on a later Exposed version: its whole value is that an *older* library
  wrote it. A new fixture for a later migration goes next to it under its own name.
- Tests never open this file directly — they copy it to a temp file first, since opening a
  database runs `SchemaUtils.create` and every test writes to it.

## How it was produced

A one-off `main` placed temporarily in the server's test source set and run through a temporary
`JavaExec` task (`classpath = sourceSets["test"].runtimeClasspath`, the target path as its single
argument). It wrote only through the production entry point and stores —
`PollenInfoDatabase.file`, `ExposedDeviceStore`, `ExposedAlarmStore`, `ExposedNotificationLog` —
with both stores on a clock fixed at `2026-10-09T08:00:00Z`. Both the `main` and the task were
deleted afterwards, so `:server:test` can never run it. Its source, verbatim:

```kotlin
package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.HIGH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.LOW
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.MODERATE
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.NONE
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.VERY_HIGH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.ALDER
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.ASH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.BEECH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.BIRCH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.GRASSES
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.HAZEL
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.OAK
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking

// TEMPORARY — one-off generator for fixtures/db/polleninfo-exposed-0.61.db, valid only on Exposed 0.61.
fun main(args: Array<String>) = runBlocking {
    val path = Path.of(args.single())
    check(!Files.exists(path)) { "$path exists" }
    val database = PollenInfoDatabase.file(path)
    val clock = Clock.fixed(Instant.parse("2026-10-09T08:00:00Z"), ZoneOffset.UTC)
    val devices = ExposedDeviceStore(database, clock)
    val alarms = ExposedAlarmStore(database, clock)
    val log = ExposedNotificationLog(database)

    val full = devices.register("fixture-token-full")
    val cleared = devices.register("fixture-token-cleared")
    devices.clearToken(cleared, "fixture-token-cleared")

    fun daily(enabled: Boolean, station: PollenStation, species: Set<PollenSpecies>, min: ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity, days: Set<DayOfWeek>, at: String) =
        AlarmSpec(enabled, station, species, min, days, AlarmSchedule.Daily(LocalTime.parse(at)))
    fun threshold(station: PollenStation, species: Set<PollenSpecies>, min: ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity, days: Set<DayOfWeek>, from: String, until: String) =
        AlarmSpec(true, station, species, min, days, AlarmSchedule.Threshold(LocalTime.parse(from), LocalTime.parse(until)))

    val fullSpecs = listOf(
        daily(true, PollenStation.ZUERICH, setOf(BIRCH, GRASSES), NONE, setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), "08:00"),
        threshold(PollenStation.BERN, setOf(HAZEL, ALDER), HIGH, DayOfWeek.entries.toSet(), "07:00", "21:00"),
        daily(false, PollenStation.LUGANO, setOf(ASH), MODERATE, setOf(SATURDAY, SUNDAY), "06:30"),
        threshold(PollenStation.GENEVE, setOf(GRASSES), VERY_HIGH, setOf(MONDAY, WEDNESDAY, FRIDAY), "09:15", "17:45"),
        daily(true, PollenStation.BASEL, setOf(OAK), LOW, setOf(TUESDAY), "12:00"),
        daily(true, PollenStation.DAVOS, PollenSpecies.entries.toSet(), NONE, DayOfWeek.entries.toSet(), "18:05"),
        daily(true, PollenStation.SION, setOf(BEECH), HIGH, setOf(THURSDAY), "23:59"),
        daily(true, PollenStation.MUENSTERLINGEN, setOf(BIRCH), VERY_HIGH, setOf(SUNDAY), "00:00"),
        daily(true, PollenStation.LUZERN, setOf(ALDER, HAZEL), MODERATE, setOf(MONDAY), "07:45"),
        daily(true, PollenStation.NEUCHATEL, setOf(GRASSES), NONE, setOf(SATURDAY), "10:10"),
    )
    val clearedSpecs = listOf(
        daily(true, PollenStation.PAYERNE, setOf(BIRCH), NONE, setOf(MONDAY), "08:00"),
        threshold(PollenStation.LAUSANNE, setOf(GRASSES, OAK), MODERATE, setOf(TUESDAY, THURSDAY), "06:00", "20:00"),
    )

    println("full device: ${full.value}")
    fullSpecs.forEach { println("  " + (alarms.create(full, it) as CreateResult.Created).alarm.id.value) }
    println("cleared device: ${cleared.value}")
    clearedSpecs.forEach { println("  " + (alarms.create(cleared, it) as CreateResult.Created).alarm.id.value) }

    val logged = alarms.list(full)!![1].id
    log.record(logged, setOf(HAZEL, ALDER), LocalDate.parse("2026-10-08"))
    log.record(logged, setOf(HAZEL), LocalDate.parse("2026-10-07"))
    println("logged: ${logged.value}")
}
```

It printed the issued ids, which the test pins:

| Device | Id | Token |
| --- | --- | --- |
| full | `Vn1knWfcI-E27R3-onc2Ug` | `fixture-token-full` |
| cleared | `MGxouTg2e1sA2e3Vu-Tyvw` | cleared through `clearToken` (`fcm_token` NULL) |

The full device's ten alarms, in creation order: `244ee5c5…`, `5268dbbe…` (the logged threshold
alert), `0635cdf3…` (paused), `4b782028…`, `79397c1b…`, `d58d7b07…`, `0da0301e…`, `fafac437…`,
`264ed258…`, `0850eb45…`; the cleared device's two: `ce133e02…`, `c333bef1…`. Full ids are in the test.
