package ch.stenzel.tim.polleninfo.core.network

import io.ktor.client.engine.HttpClientEngine

expect fun httpClientEngine(): HttpClientEngine
