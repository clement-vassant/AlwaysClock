package io.shodo.standbyclock

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L'app marche sur tout appareil Android : aucun texte visible ne doit la présenter comme réservée au Fold. */
class VisibleTextsTest {
    // Les tests unitaires tournent depuis le dossier du module app.
    private val strings = File("src/main/res/values/strings.xml").readText()
    private val settings = File("src/main/java/io/shodo/standbyclock/SettingsActivity.kt").readText()

    @Test
    fun `no device-specific wording in visible texts`() {
        for (word in listOf("Fold", "Galaxy")) {
            assertFalse("strings.xml mentionne $word", word in strings)
            assertFalse("SettingsActivity mentionne $word", word in settings)
        }
    }

    @Test
    fun `routines help is generic`() {
        val routines = Regex("<string name=\"help_routines\">(.*)</string>").find(strings)!!.groupValues[1]
        assertTrue(routines, "Tasker" in routines)
        assertTrue(routines, "routines ou outils d\\'automatisation" in routines)
    }
}
