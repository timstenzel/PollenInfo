package ch.stenzel.tim.polleninfo.core.network

import org.koin.core.qualifier.named

/**
 * The Koin qualifier of the base address of **our own** backend (`:server`), a `String` without a
 * trailing slash.
 *
 * The address comes from the platform entry point — `appModules(apiBaseUrl)` — rather than from a
 * constant here: Android takes it from the build type's `BuildConfig.API_BASE_URL` (the deployed
 * HTTPS backend for a release, the host machine's development server for a debug build), iOS from
 * `MainViewController`. Consumers never read it themselves — Koin passes it to the API services
 * that need it, so a test can construct a service pointing at any host.
 */
val API_BASE_URL = named("apiBaseUrl")
