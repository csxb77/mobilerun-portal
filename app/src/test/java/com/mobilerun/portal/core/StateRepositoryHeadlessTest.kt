package com.mobilerun.portal.core

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.mobilerun.portal.service.MobilerunAccessibilityService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class StateRepositoryHeadlessTest {
    @Test
    fun nullServiceKeepsHeadlessBehavior() {
        val repository = StateRepository(service = null)
        val phoneState = repository.getPhoneState()

        assertFalse(repository.hasAccessibilityService)
        assertTrue(repository.getVisibleElements().isEmpty())
        assertNull(repository.getFullTree(filter = true))
        assertFalse(repository.setOverlayVisible(true))
        assertFalse(repository.inputText("hello", clear = true))
        assertNull(phoneState.packageName)
        assertFalse(phoneState.keyboardVisible)
        assertTrue(repository.takeScreenshot(hideOverlay = false).isCompletedExceptionally)
    }

    @Test
    fun fullTreeFallbackSkipsUserFacingWindowsWithNullRoots() {
        val service = mockk<MobilerunAccessibilityService>()
        val emptyTopWindow = mockk<AccessibilityWindowInfo>()
        val appWindow = mockk<AccessibilityWindowInfo>()
        val root = mockk<AccessibilityNodeInfo>()
        val expected = JSONObject().put("ok", true)

        every { service.rootInActiveWindow } returns null
        every { service.windows } returns listOf(emptyTopWindow, appWindow)
        every { service.getScreenBounds() } returns Rect(0, 0, 100, 100)
        every { emptyTopWindow.layer } returns 2
        every { emptyTopWindow.type } returns AccessibilityWindowInfo.TYPE_APPLICATION
        every { emptyTopWindow.root } returns null
        every { emptyTopWindow.recycle() } just runs
        every { appWindow.layer } returns 1
        every { appWindow.type } returns AccessibilityWindowInfo.TYPE_APPLICATION
        every { appWindow.root } returns root
        every { appWindow.recycle() } just runs

        mockkObject(AccessibilityTreeBuilder)
        try {
            every {
                AccessibilityTreeBuilder.buildFullAccessibilityTreeJson(root, any())
            } returns expected

            assertSame(expected, StateRepository(service).getFullTree(filter = true))

            verify { emptyTopWindow.root }
            verify { appWindow.root }
        } finally {
            unmockkObject(AccessibilityTreeBuilder)
        }
    }

    @Test
    fun fullTreeFallbackPrefersApplicationRootOverHigherLayerSystemRoot() {
        val service = mockk<MobilerunAccessibilityService>()
        val systemWindow = mockk<AccessibilityWindowInfo>()
        val appWindow = mockk<AccessibilityWindowInfo>()
        val systemRoot = mockk<AccessibilityNodeInfo>()
        val appRoot = mockk<AccessibilityNodeInfo>()
        val expected = JSONObject().put("app", true)

        every { service.rootInActiveWindow } returns null
        every { service.windows } returns listOf(systemWindow, appWindow)
        every { service.getScreenBounds() } returns Rect(0, 0, 100, 100)
        every { systemWindow.layer } returns 10
        every { systemWindow.type } returns AccessibilityWindowInfo.TYPE_SYSTEM
        every { systemWindow.root } returns systemRoot
        every { systemWindow.recycle() } just runs
        every { appWindow.layer } returns 1
        every { appWindow.type } returns AccessibilityWindowInfo.TYPE_APPLICATION
        every { appWindow.root } returns appRoot
        every { appWindow.recycle() } just runs

        mockkObject(AccessibilityTreeBuilder)
        try {
            every {
                AccessibilityTreeBuilder.buildFullAccessibilityTreeJson(appRoot, any())
            } returns expected

            assertSame(expected, StateRepository(service).getFullTree(filter = true))

            verify { appWindow.root }
            verify(exactly = 0) { systemWindow.root }
        } finally {
            unmockkObject(AccessibilityTreeBuilder)
        }
    }

    @Test
    fun fullTreeFallbackUsesSystemRootWhenNoApplicationRootExists() {
        val service = mockk<MobilerunAccessibilityService>()
        val systemWindow = mockk<AccessibilityWindowInfo>()
        val systemRoot = mockk<AccessibilityNodeInfo>()
        val expected = JSONObject().put("system", true)

        every { service.rootInActiveWindow } returns null
        every { service.windows } returns listOf(systemWindow)
        every { service.getScreenBounds() } returns Rect(0, 0, 100, 100)
        every { systemWindow.layer } returns 1
        every { systemWindow.type } returns AccessibilityWindowInfo.TYPE_SYSTEM
        every { systemWindow.root } returns systemRoot
        every { systemWindow.recycle() } just runs

        mockkObject(AccessibilityTreeBuilder)
        try {
            every {
                AccessibilityTreeBuilder.buildFullAccessibilityTreeJson(systemRoot, any())
            } returns expected

            assertSame(expected, StateRepository(service).getFullTree(filter = true))

            verify { systemWindow.root }
        } finally {
            unmockkObject(AccessibilityTreeBuilder)
        }
    }
}
