package io.shodo.standbyclock

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityOrientationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPrefs() {
        context.getSharedPreferences("standby_clock", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun requestedOrientationWith(mode: OrientationMode): Int {
        ClockPrefs(context).orientation = mode
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var requested = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            scenario.onActivity { requested = it.requestedOrientation }
            return requested
        }
    }

    @Test
    fun automaticFollowsThePhoneRotation() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, requestedOrientationWith(OrientationMode.AUTO))
    }

    @Test
    fun alwaysLandscapeKeepsTheAppInLandscape() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, requestedOrientationWith(OrientationMode.LANDSCAPE))
    }
}
