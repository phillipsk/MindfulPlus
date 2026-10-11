package com.example.mindfulplus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

/** Exercises the user journey and verifies that revoking access protects an already-open screen. */
@RunWith(AndroidJUnit4::class)
class DemoJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var app: MindfulApplication

    @Before fun setup() {
        assumeTrue("Demo journey runs in demoDebug", BuildConfig.DEMO_MODE)
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("journey", 0).edit().clear().commit()
        app.getSharedPreferences("demo_billing", 0).edit().clear().commit()
        app.billing.resetDemo()
        app.billing.clearNotice()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
    @After fun teardown() { if (::scenario.isInitialized) scenario.close() }

    private fun tap(text: String) { compose.onNodeWithText(text, useUnmergedTree = false).performScrollTo().performClick() }
    private fun capture(name: String) {
        val file = java.io.File(app.externalCacheDir, "previews/$name.png")
        file.parentFile?.mkdirs()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun onboardingPaywallPurchaseRestoreAndRevocation() {
        capture("welcome")
        tap("Find my rhythm")
        tap("Find focus")
        tap("Continue")
        tap("Let’s begin")
        capture("home")
        tap("Begin session")
        tap("Next step")
        tap("Next step")
        tap("Finish session")
        compose.onNodeWithText("Simulate Plus purchase").assertExists()
        capture("paywall")
        tap("Maybe later")
        tap("Back to today")
        compose.onNodeWithText("Explore").performClick()
        capture("library")
        compose.onNodeWithTag("session_focus").performScrollTo().performClick()
        tap("Simulate Plus purchase")
        compose.onNodeWithText("Choose one thing").assertExists()
        compose.runOnIdle { app.billing.resetDemo() }
        compose.onNodeWithText("Explore Plus").assertExists()
        tap("Explore Plus")
        tap("Simulate Plus purchase")
        compose.onNodeWithText("Choose one thing").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("You").performClick()
        tap("Restore purchases")
        compose.onNodeWithText("MindfulPlus member").assertExists()
        compose.onNodeWithText("Progress").performClick()
        compose.onNodeWithText("1").assertExists()
        // A fresh Activity retains onboarding, progress, and the simulated entitlement.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.onNodeWithText("Begin session").assertExists()
        compose.onNodeWithText("You").performClick()
        compose.onNodeWithText("MindfulPlus member").assertExists()
    }
}
