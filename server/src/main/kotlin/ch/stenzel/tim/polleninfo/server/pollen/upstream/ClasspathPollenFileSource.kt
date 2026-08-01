package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation

/**
 * Serves checked-in copies of the published files from the classpath.
 *
 * This is the walking skeleton's file source: it lets the whole path — parse, classify, serve,
 * render — run end to end before any HTTP client exists. The samples under [ROOT] are verbatim
 * downloads of the real files, so the column layout and encoding under test are the real ones; only
 * their age and the transport are fake.
 *
 * The path is [PollenStation.hourlyNowPath], the same relative path the published service uses, so
 * swapping in the HTTP implementation is a change of prefix and nothing else.
 */
class ClasspathPollenFileSource(
    private val root: String = ROOT,
) : PollenFileSource {

    override suspend fun hourlyNow(station: PollenStation): ByteArray {
        val path = "$root/${station.hourlyNowPath}"
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw NoSuchElementException("no checked-in sample at $path")
        return stream.use { it.readBytes() }
    }

    companion object {
        const val ROOT = "fixtures/ogd-pollen"
    }
}
