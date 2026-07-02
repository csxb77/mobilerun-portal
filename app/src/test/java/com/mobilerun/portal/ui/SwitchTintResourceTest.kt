package com.mobilerun.portal.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SwitchTintResourceTest {
    private val resDir: File = locateResDir()

    @Test
    fun switchStyleUsesExplicitTintSelectors() {
        val themes = resource("values/themes.xml").readText()

        assertTrue(themes.contains("<item name=\"useMaterialThemeColors\">false</item>"))
        assertTrue(themes.contains("<item name=\"thumbTint\">@color/mobilerun_switch_thumb_tint</item>"))
        assertTrue(themes.contains("<item name=\"trackTint\">@color/mobilerun_switch_track_tint</item>"))
    }

    @Test
    fun trackTintSeparatesCheckedAndUncheckedStates() {
        val track = resource("color/mobilerun_switch_track_tint.xml").readText()

        assertTrue(track.contains("android:state_enabled=\"false\""))
        assertTrue(track.contains("android:color=\"@color/text_gray_dark\""))
        assertTrue(track.contains("android:state_checked=\"true\""))
        assertTrue(track.contains("android:color=\"@color/mobilerun_primary\""))
        assertTrue(track.contains("android:color=\"@color/text_gray_light\""))

        val checkedColor = colorValue("mobilerun_primary")
        val uncheckedColor = colorValue("text_gray_light")
        assertEquals("#0D9373", checkedColor)
        assertEquals("#AAAAAA", uncheckedColor)
        assertNotEquals(checkedColor, uncheckedColor)
    }

    @Test
    fun thumbTintUsesWhiteWhenEnabledAndGrayWhenDisabled() {
        val thumb = resource("color/mobilerun_switch_thumb_tint.xml").readText()

        assertTrue(thumb.contains("android:state_enabled=\"false\""))
        assertTrue(thumb.contains("android:color=\"@color/text_gray\""))
        assertTrue(thumb.contains("android:color=\"@color/white\""))
    }

    private fun resource(path: String): File = File(resDir, path)

    private fun colorValue(name: String): String {
        val colors = resource("values/colors.xml").readText()
        val regex = Regex("""<color\s+name="$name">([^<]+)</color>""")
        return requireNotNull(regex.find(colors)) {
            "Missing color resource: $name"
        }.groupValues[1].uppercase()
    }

    private fun locateResDir(): File {
        val start = File(System.getProperty("user.dir") ?: error("user.dir is unavailable"))
            .absoluteFile
        return generateSequence(start) { it.parentFile }
            .map { File(it, "src/main/res") }
            .first { File(it, "values/themes.xml").isFile }
    }
}
