package com.example.mindfulplus.billing

import org.junit.Assert.*
import org.junit.Test

class AccessPolicyTest {
    @Test fun replacingDifferentSubscriptionProductsNeedsOldPurchase() {
        assertTrue(AccessPolicy.shouldReplaceSubscription("monthly", "monthly", "annual", "annual"))
    }
    @Test fun changingBasePlansOfTheSameProductNeedsReplacement() {
        assertTrue(AccessPolicy.shouldReplaceSubscription("plus", "monthly", "plus", "annual"))
    }
    @Test fun theSameProductAndBasePlanDoNotNeedReplacement() {
        assertFalse(AccessPolicy.shouldReplaceSubscription("plus", "annual", "plus", "annual"))
        assertFalse(AccessPolicy.shouldReplaceSubscription("plus", null, "plus", null))
    }
    @Test fun onlyTheConfiguredEntitlementUnlocksAccess() {
        assertTrue(AccessPolicy.hasAccess(setOf("mindful_plus", "other"), "mindful_plus"))
        assertFalse(AccessPolicy.hasAccess(setOf("other"), "mindful_plus"))
        assertFalse(AccessPolicy.hasAccess(setOf("mindful_plus_trial"), "mindful_plus"))
    }
    @Test fun missingEntitlementsNeverUnlockAccess() {
        assertFalse(AccessPolicy.hasAccess(emptySet(), "mindful_plus"))
        assertFalse(AccessPolicy.hasAccess(setOf(""), ""))
    }
    @Test fun firstCompletedFreeSessionOffersUpgrade() {
        assertFalse(AccessPolicy.shouldOfferUpgrade(0, false))
        assertTrue(AccessPolicy.shouldOfferUpgrade(1, false))
        assertTrue(AccessPolicy.shouldOfferUpgrade(3, false))
    }
    @Test fun membersAreNotPromptedAfterSessions() {
        assertFalse(AccessPolicy.shouldOfferUpgrade(1, true))
        assertFalse(AccessPolicy.shouldOfferUpgrade(10, true))
    }
    @Test fun missingKeyProducesActionableSetupError() {
        assertNotNull(AccessPolicy.configurationError("", "mindful_plus", true))
    }
    @Test fun secretKeysAreRejected() {
        assertNotNull(AccessPolicy.configurationError("sk_secret", "mindful_plus", true))
    }
    @Test fun testStoreKeysAreAllowedOnlyInDebug() {
        assertNull(AccessPolicy.configurationError("test_public", "mindful_plus", true))
        assertNotNull(AccessPolicy.configurationError("test_public", "mindful_plus", false))
    }
    @Test fun googlePlayKeysCanConfigureReleaseBuilds() {
        assertNull(AccessPolicy.configurationError("goog_public", "mindful_plus", false))
    }
    @Test fun missingEntitlementIsRejected() {
        assertNotNull(AccessPolicy.configurationError("goog_public", "", true))
    }
}
