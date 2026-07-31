package ch.stenzel.tim.polleninfo.core.network

/**
 * The iOS simulator shares the host machine's network stack, so the development server is simply
 * on `localhost`. Reaching it over cleartext needs an App Transport Security exception
 * (`NSAllowsLocalNetworking`) in the future iOS wrapper's `Info.plist` — see `CLAUDE.md`.
 */
actual val apiBaseUrl: String = "http://localhost:8080"
