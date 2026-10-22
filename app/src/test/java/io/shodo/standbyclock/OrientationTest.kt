package io.shodo.standbyclock

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationTest {
    @Test
    fun `automatic follows the phone rotation`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, activityOrientation(OrientationMode.AUTO))
    }

    @Test
    fun `always landscape keeps both landscape sides`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activityOrientation(OrientationMode.LANDSCAPE))
    }
}
