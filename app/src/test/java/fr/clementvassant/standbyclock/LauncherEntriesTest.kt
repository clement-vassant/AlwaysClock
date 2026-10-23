package fr.clementvassant.standbyclock

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Une seule icône dans le lanceur : trois entrées « StandBy » à côté l'une de l'autre déroutaient. */
class LauncherEntriesTest {
    // Les tests unitaires tournent depuis le dossier du module app.
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `only one launcher entry`() {
        assertEquals(1, Regex("android.intent.category.LAUNCHER").findAll(manifest).count())
        // Un alias exporté se lance aussi par le nom du paquet : une routine pouvait tomber sur « Horloge jour ».
        assertFalse("<activity-alias" in manifest)
    }
}
