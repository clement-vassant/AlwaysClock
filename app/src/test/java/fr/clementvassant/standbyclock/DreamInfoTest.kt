package fr.clementvassant.standbyclock

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sans image d'aperçu, les réglages Android listent l'écran de veille avec une vignette vide. */
class DreamInfoTest {
    // Les tests unitaires tournent depuis le dossier du module app.
    private val dreamInfo = File("src/main/res/xml/dream_info.xml").readText()

    @Test
    fun `screen saver has a preview image`() {
        assertTrue(dreamInfo, "android:previewImage=\"@drawable/dream_preview\"" in dreamInfo)
        assertTrue(File("src/main/res/drawable/dream_preview.xml").isFile)
    }
}
