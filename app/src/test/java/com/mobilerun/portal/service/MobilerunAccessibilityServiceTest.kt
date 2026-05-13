package com.mobilerun.portal.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilerunAccessibilityServiceTest {

    @Test
    fun shouldReuseVisibleElementsSnapshot_allowsFreshSameScreenCache() {
        val result = MobilerunAccessibilityService.shouldReuseVisibleElementsSnapshot(
            cachedElementCount = 1,
            snapshotTimeMs = 1_000L,
            nowMs = 1_000L + MobilerunAccessibilityService.VISIBLE_ELEMENTS_STALE_GRACE_MS,
            snapshotPackageName = "com.example.app",
            currentPackageName = "com.example.app",
            snapshotActivityName = "MainActivity",
            currentActivityName = "MainActivity",
        )

        assertTrue(result)
    }

    @Test
    fun shouldReuseVisibleElementsSnapshot_rejectsExpiredCache() {
        val result = MobilerunAccessibilityService.shouldReuseVisibleElementsSnapshot(
            cachedElementCount = 1,
            snapshotTimeMs = 1_000L,
            nowMs = 1_001L + MobilerunAccessibilityService.VISIBLE_ELEMENTS_STALE_GRACE_MS,
            snapshotPackageName = "com.example.app",
            currentPackageName = "com.example.app",
            snapshotActivityName = "MainActivity",
            currentActivityName = "MainActivity",
        )

        assertFalse(result)
    }

    @Test
    fun shouldReuseVisibleElementsSnapshot_rejectsPackageOrActivityMismatch() {
        val packageMismatch = MobilerunAccessibilityService.shouldReuseVisibleElementsSnapshot(
            cachedElementCount = 1,
            snapshotTimeMs = 1_000L,
            nowMs = 1_100L,
            snapshotPackageName = "com.example.app",
            currentPackageName = "com.example.other",
            snapshotActivityName = "MainActivity",
            currentActivityName = "MainActivity",
        )
        val activityMismatch = MobilerunAccessibilityService.shouldReuseVisibleElementsSnapshot(
            cachedElementCount = 1,
            snapshotTimeMs = 1_000L,
            nowMs = 1_100L,
            snapshotPackageName = "com.example.app",
            currentPackageName = "com.example.app",
            snapshotActivityName = "MainActivity",
            currentActivityName = "OtherActivity",
        )

        assertFalse(packageMismatch)
        assertFalse(activityMismatch)
    }

    @Test
    fun shouldReuseVisibleElementsSnapshot_rejectsEmptyCache() {
        val result = MobilerunAccessibilityService.shouldReuseVisibleElementsSnapshot(
            cachedElementCount = 0,
            snapshotTimeMs = 1_000L,
            nowMs = 1_100L,
            snapshotPackageName = "com.example.app",
            currentPackageName = "com.example.app",
            snapshotActivityName = "MainActivity",
            currentActivityName = "MainActivity",
        )

        assertFalse(result)
    }

    @Test
    fun testCalculateInputText_Replace() {
        // clear=true should always return newText
        val result = MobilerunAccessibilityService.calculateInputText("old", "hint", "new", true)
        assertEquals("new", result)
    }

    @Test
    fun testCalculateInputText_Append() {
        // clear=false should append
        val result = MobilerunAccessibilityService.calculateInputText("old", "hint", "new", false)
        assertEquals("oldnew", result)
    }

    @Test
    fun testCalculateInputText_AppendWithNulls() {
        val result = MobilerunAccessibilityService.calculateInputText(null, null, "new", false)
        assertEquals("new", result)
    }

    @Test
    fun testCalculateInputText_SmartHint() {
        // Case: Text equals Hint -> Treat as empty (Prevent "Searchsome-text")
        val result = MobilerunAccessibilityService.calculateInputText("Search", "Search", "query", false)
        assertEquals("query", result)
    }

    @Test
    fun testCalculateInputText_SmartHintMismatch() {
        // Case: Text does NOT equal Hint -> Append normally
        val result = MobilerunAccessibilityService.calculateInputText("Existing", "Search", "query", false)
        assertEquals("Existingquery", result)
    }

    @Test
    fun testCalculateInputText_InsertAtCursor() {
        val result = MobilerunAccessibilityService.calculateInputText(
            currentText = "hello world",
            hintText = null,
            newText = " brave",
            clear = false,
            selectionStart = 5,
            selectionEnd = 5,
        )
        assertEquals("hello brave world", result)
    }

    @Test
    fun testCalculateInputText_ReplaceSelection() {
        val result = MobilerunAccessibilityService.calculateInputText(
            currentText = "hello world",
            hintText = null,
            newText = "planet",
            clear = false,
            selectionStart = 6,
            selectionEnd = 11,
        )
        assertEquals("hello planet", result)
    }

    @Test
    fun testCalculateDeleteText_BackspaceAtCursor() {
        val result = MobilerunAccessibilityService.calculateDeleteText(
            currentText = "hello",
            hintText = null,
            count = 1,
            forward = false,
            selectionStart = 3,
            selectionEnd = 3,
        )
        assertEquals("helo", result)
    }

    @Test
    fun testCalculateDeleteText_DeleteSelection() {
        val result = MobilerunAccessibilityService.calculateDeleteText(
            currentText = "hello world",
            hintText = null,
            count = 1,
            forward = false,
            selectionStart = 5,
            selectionEnd = 11,
        )
        assertEquals("hello", result)
    }

    @Test
    fun testCalculateDeleteText_ForwardDeleteAtCursor() {
        val result = MobilerunAccessibilityService.calculateDeleteText(
            currentText = "hello",
            hintText = null,
            count = 1,
            forward = true,
            selectionStart = 2,
            selectionEnd = 2,
        )
        assertEquals("helo", result)
    }

    @Test
    fun testCalculateDeleteText_DeleteReversedSelection() {
        val result = MobilerunAccessibilityService.calculateDeleteText(
            currentText = "hello world",
            hintText = null,
            count = 1,
            forward = false,
            selectionStart = 11,
            selectionEnd = 5,
        )
        assertEquals("hello", result)
    }
}
