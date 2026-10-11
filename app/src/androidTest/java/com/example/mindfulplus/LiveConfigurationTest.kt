package com.example.mindfulplus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

/** A fresh live build without credentials must stay usable and never substitute simulated billing. */
@RunWith(AndroidJUnit4::class)
class LiveConfigurationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    @Before fun setup() {
        assumeTrue(!BuildConfig.DEMO_MODE && BuildConfig.RC_PUBLIC_SDK_KEY.isBlank())
        val app = ApplicationProvider.getApplicationContext<MindfulApplication>()
        app.getSharedPreferences("journey", 0).edit().clear().commit()
        assertFalse(app.billing.state.value.isDemo)
        assertFalse(app.billing.state.value.premium)
        assertTrue(app.billing.state.value.plans.isEmpty())
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
    @After fun teardown() { if (::scenario.isInitialized) scenario.close() }
    private fun tap(text: String) { compose.onNodeWithText(text).performScrollTo().performClick() }
    @Test fun missingKeyShowsSetupAndDisablesCheckout() {
        tap("Find my rhythm")
        tap("Continue")
        tap("Let’s begin")
        compose.onNodeWithText("Explore").performClick()
        compose.onNodeWithTag("session_focus").performScrollTo().performClick()
        compose.onNodeWithText("Connect your RevenueCat project").assertExists()
        compose.onNodeWithText("Continue to checkout").performScrollTo().assertIsNotEnabled()
        tap("Maybe later")
        compose.onNodeWithTag("session_morning").performScrollTo().performClick()
        compose.onNodeWithText("Settle in").assertExists()
    }
}
