package io.shodo.standbyclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// La version vient de -PappVersion (release.yml) : ces tests cassent si elle n'est plus du SemVer
// ou si versionCode ne suit plus versionName, ce qui bloquerait les mises à jour sur le téléphone.
class AppVersionTest {
    @Test
    fun `version name is SemVer`() {
        assertTrue(BuildConfig.VERSION_NAME, Regex("""\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?""").matches(BuildConfig.VERSION_NAME))
    }

    @Test
    fun `version code grows with version name`() {
        val (major, minor, patch) = BuildConfig.VERSION_NAME.substringBefore("-").split(".").map(String::toInt)
        assertEquals(major * 10_000 + minor * 100 + patch, BuildConfig.VERSION_CODE)
    }
}
