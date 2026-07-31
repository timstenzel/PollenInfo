package ch.stenzel.tim.polleninfo.core.network

/**
 * Base address of **our own** backend (`:server`), without a trailing slash.
 *
 * It differs per platform because a development server on the host machine is reachable under a
 * different name from each emulator/simulator, so there is one actual per platform rather than one
 * shared constant. Consumers never read this directly — Koin passes it to the API services that
 * need it, so a test can construct a service pointing at any host.
 */
expect val apiBaseUrl: String
