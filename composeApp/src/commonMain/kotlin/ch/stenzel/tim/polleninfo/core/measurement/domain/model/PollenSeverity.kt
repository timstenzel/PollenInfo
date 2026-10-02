package ch.stenzel.tim.polleninfo.core.measurement.domain.model

/**
 * Severity band of a pollen concentration, as classified by the backend.
 *
 * **Declared here as well as in `:server`, deliberately.** `:composeApp` cannot depend on the
 * server module, and the alternative — a shared multiplatform domain module — would make `:server`
 * depend on one for the first time over five constants. This is the pattern the project already
 * uses: `StationDto` exists on both sides too. The contract between them is the constant *name* on
 * the wire (`"VERY_HIGH"`), pinned by `StationMeasurementMapperTest`, which is what makes a rename
 * a failing test rather than a runtime surprise.
 *
 * Declared least to most severe, so `maxOrNull()` over a list picks the worst.
 *
 * The app never classifies a concentration itself — it renders the band the server computed, so
 * every client agrees on what "High" means.
 */
enum class PollenSeverity {
    NONE,
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
}
