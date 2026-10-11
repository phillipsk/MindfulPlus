package com.example.mindfulplus.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.revenuecat.purchases.CustomerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Local simulation. This class never creates RevenueCat customers or store transactions. */
class DemoBillingRepository(private val preferences: SharedPreferences) : BillingRepository {
    private val mutable = MutableStateFlow(BillingState(
        isDemo = true,
        premium = preferences.getBoolean("demo_premium", false),
        plans = listOf(
            Plan("annual", "Annual", "$39.99", "per year · example price"),
            Plan("monthly", "Monthly", "$5.99", "per month · example price"),
        ),
        offerings = listOf("demo_default", "demo_focus"),
        metadata = "theme: forest; campaign: first_session (simulated)",
        currencies = listOf("Calm credits · 120 (simulated)"),
        events = listOf("Local demo initialized. No SDK requests or charges."),
    ))
    override val state = mutable.asStateFlow()
    override fun refresh(force: Boolean) { record("Demo customer state refreshed") }
    override fun purchase(activity: Activity, plan: Plan) {
        preferences.edit().putBoolean("demo_premium", true).apply()
        mutable.update { it.copy(premium = true, subscriptionSummary = "${plan.title} · simulated membership", notice = "Demo Plus unlocked. No payment was made.") }
        record("Simulated ${plan.title} purchase")
    }
    override fun restore() {
        val restored = preferences.getBoolean("demo_premium", false)
        mutable.update { it.copy(premium = restored, notice = if (restored) "Simulated membership restored." else "No simulated purchase to restore.") }
        record("Demo restore: access=$restored")
    }
    override fun login(userId: String) {
        if (userId.isBlank()) return
        mutable.update { it.copy(userId = userId.trim(), anonymous = false, notice = "Demo identity changed. No authentication was performed.") }
        record("Demo identity selected")
    }
    override fun logout() { mutable.update { it.copy(userId = "Anonymous", anonymous = true, notice = "Demo identity reset.") }; record("Demo logout") }
    override fun saveAttributes(displayName: String, email: String, goal: String) { mutable.update { it.copy(notice = "Attributes simulated locally. Nothing was sent.") }; record("Demo attributes updated: goal=$goal") }
    override fun loadOffering(id: String) { record("Demo offering selected: $id") }
    override fun lookupProducts() { mutable.update { it.copy(products = listOf("demo_annual · $39.99 (example)", "demo_monthly · $5.99 (example)")) }; record("Demo product lookup") }
    override fun refreshCurrencies() { record("Demo currency balances refreshed") }
    override fun manageSubscriptions(context: Context) { mutable.update { it.copy(notice = "Demo subscriptions have no store management page. You can reset demo access in the SDK lab.") } }
    override fun checkPayments(context: Context) { mutable.update { it.copy(notice = "Demo mode supports simulated purchases. Live mode checks store billing availability.") } }
    override fun acceptCustomerInfo(info: CustomerInfo, source: String) = Unit
    override fun record(event: String) { mutable.update { it.copy(events = (listOf(event) + it.events).take(40)) } }
    override fun clearNotice() { mutable.update { it.copy(notice = null) } }
    override fun resetDemo() {
        preferences.edit().remove("demo_premium").apply()
        mutable.update { it.copy(premium = false, subscriptionSummary = "Free membership", notice = "Demo access reset.") }
        record("Demo entitlement removed")
    }
}
