package com.example.mindfulplus.billing

/** Access always follows the configured entitlement, never a successful payment alone. */
object AccessPolicy {
    fun hasAccess(activeEntitlements: Set<String>, requiredEntitlement: String): Boolean =
        requiredEntitlement.isNotBlank() && requiredEntitlement in activeEntitlements

    fun shouldOfferUpgrade(completedSessions: Int, hasAccess: Boolean): Boolean =
        completedSessions >= 1 && !hasAccess

    fun shouldReplaceSubscription(oldProduct: String, oldBasePlan: String?, newProduct: String, newBasePlan: String?): Boolean =
        oldProduct != newProduct || (oldBasePlan != null && newBasePlan != null && oldBasePlan != newBasePlan)

    fun configurationError(key: String, entitlement: String, debug: Boolean): String? = when {
        key.isBlank() -> "Add your public Android SDK key to revenuecat.properties, then rebuild the live variant."
        !key.startsWith("goog_") && !key.startsWith("test_") -> "Use a goog_ or test_ public SDK key. Secret API keys do not belong in an Android app."
        key.startsWith("test_") && !debug -> "Test Store keys are enabled only in debug builds."
        entitlement.isBlank() -> "Set the entitlement ID in revenuecat.properties."
        else -> null
    }
}
