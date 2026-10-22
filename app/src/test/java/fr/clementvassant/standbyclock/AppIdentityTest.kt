package fr.clementvassant.standbyclock

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Projet personnel : l'identifiant de l'app et le dépôt ne doivent porter le nom d'aucun employeur. */
class AppIdentityTest {
    @Test
    fun `application id is the personal one`() {
        // Changer l'applicationId crée une autre app pour Android : la version installée ne se mettrait plus à jour.
        assertEquals("fr.clementvassant.standbyclock", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `no employer name left in the repository`() {
        // Les tests unitaires tournent depuis le dossier du module app : la racine du dépôt est juste au-dessus.
        val repoRoot = File("..").canonicalFile
        val skippedDirs = setOf(".git", ".gradle", "build", ".idea")
        val offenders =
            repoRoot
                .walkTopDown()
                .onEnter { it.name !in skippedDirs }
                .filter { it.isFile && it.extension in setOf("kt", "kts", "xml", "md", "yml", "sh", "toml", "properties") }
                .filter { it.name != "AppIdentityTest.kt" && FORBIDDEN_NAME in it.readText().lowercase() }
                .map { it.relativeTo(repoRoot).path }
                .toList()
        assertTrue("« $FORBIDDEN_NAME » apparaît dans : $offenders", offenders.isEmpty())
    }

    private companion object {
        const val FORBIDDEN_NAME = "shodo"
    }
}
