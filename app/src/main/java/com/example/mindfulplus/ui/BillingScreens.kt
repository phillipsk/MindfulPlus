package com.example.mindfulplus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.mindfulplus.BuildConfig
import com.example.mindfulplus.billing.*
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package as RCPackage
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.*
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterOptions

private fun readablePeriod(period: String): String = when (period) {
    "P1Y" -> "Billed once per year"
    "P1M" -> "Billed once per month"
    "P1W" -> "Billed once per week"
    "P3M" -> "Billed every 3 months"
    "P6M" -> "Billed every 6 months"
    else -> period
}

@Composable
fun CustomPaywallScreen(state: BillingState, close: () -> Unit, restore: () -> Unit, buy: (Plan) -> Unit,
    hosted: () -> Unit, retry: () -> Unit, policy: (String) -> Unit) {
    var selected by remember(state.plans) { mutableStateOf(state.plans.firstOrNull()?.id) }
    Page {
        PageHeader("MindfulPlus", close)
        ModeBadge(state.isDemo)
        Text("More space\nfor what matters.", style = MaterialTheme.typography.displaySmall)
        Text("Build your own rhythm with the full collection.", color = Muted)
        listOf("Every focus and wind-down session", "A practice for each part of your day", "New room to explore at your own pace").forEach { benefit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Forest, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp)); Text(benefit, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (!state.configured) InfoCard("Connect your RevenueCat project", state.configurationError ?: "Configuration required.")
        if (state.plans.isEmpty() && state.configured && !state.busy) {
            InfoCard("Plans are not available yet", "Check your current offering, packages, store products, and device connection. Free sessions are still available.")
            OutlinedButton(onClick = retry, modifier = Modifier.fillMaxWidth()) { Text("Reload plans") }
        }
        state.plans.forEach { plan ->
            Surface(Modifier.fillMaxWidth().testTag("plan_${plan.id}").clickable(enabled = !state.busy) { selected = plan.id },
                shape = RoundedCornerShape(20.dp), color = if (selected == plan.id) Lime else Soft,
                border = if (selected == plan.id) BorderStroke(1.dp, Forest) else null) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == plan.id, onClick = { selected = plan.id }, enabled = !state.busy)
                    Column(Modifier.weight(1f)) {
                        Text(plan.title, style = MaterialTheme.typography.titleMedium)
                        Text(readablePeriod(plan.period), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text(plan.price, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        val plan = state.plans.firstOrNull { it.id == selected }
        PrimaryButton(if (state.busy) "Please wait…" else if (state.isDemo) "Simulate Plus purchase" else if (state.premium) "Continue with this plan" else "Continue to checkout",
            enabled = plan != null && !state.busy && state.configured) { plan?.let(buy) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(if (state.isDemo) "Example prices only. This demo purchase unlocks local sample content and never charges you."
            else "Subscriptions renew automatically unless canceled in your store settings. Store checkout shows the final price, billing schedule, and any eligible introductory offer. Your membership follows the Plus entitlement.",
            style = MaterialTheme.typography.bodySmall, color = Muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = restore, enabled = !state.busy && state.configured) { Text("Restore purchases") }
            TextButton(onClick = close) { Text("Maybe later") }
        }
        OutlinedButton(onClick = hosted, enabled = state.configured && !state.busy && (state.isDemo || state.offering != null), modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isDemo) "Preview hosted paywall setup" else "Show RevenueCat paywall")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { policy("terms") }) { Text("Terms") }
            TextButton(onClick = { policy("privacy") }) { Text("Privacy") }
        }
    }
}

@Composable
fun HostedPaywallScreen(state: BillingState, billing: BillingRepository, close: () -> Unit) {
    if (state.isDemo || !state.configured || state.offering == null) {
        Page {
            PageHeader("RevenueCat paywall", close)
            BotanicalArt()
            Text("Your paywall.\nRemotely managed.", style = MaterialTheme.typography.headlineLarge)
            InfoCard("Live setup required", "Choose liveDebug, add a public SDK key, and attach a published paywall to your offering in RevenueCat. This screen then renders the SDK’s actual Compose paywall.")
            Text("The custom demo paywall is available now. Hosted paywalls require a connected RevenueCat project.", color = Muted)
            PrimaryButton("Back to plans", onClick = close)
        }
        return
    }
    val listener = remember(billing) {
        object : PaywallListener {
            override fun onPurchaseStarted(rcPackage: RCPackage) { billing.record("Hosted paywall: purchase started") }
            override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                billing.acceptCustomerInfo(customerInfo, "Hosted paywall purchase")
                billing.refreshCurrencies()
            }
            override fun onPurchaseCancelled() { billing.record("Hosted paywall: purchase canceled") }
            override fun onPurchaseError(error: PurchasesError) { billing.record("Hosted paywall purchase error: ${error.code}") }
            override fun onRestoreStarted() { billing.record("Hosted paywall: restore started") }
            override fun onRestoreCompleted(customerInfo: CustomerInfo) { billing.acceptCustomerInfo(customerInfo, "Hosted paywall restore") }
            override fun onRestoreError(error: PurchasesError) { billing.record("Hosted paywall restore error: ${error.code}") }
            override fun onWebCheckoutOpened() { billing.record("Hosted paywall: web checkout opened") }
            override fun onUrlOpened(url: String) { billing.record("Hosted paywall: policy or destination link opened") }
            override fun onInteraction(event: PaywallInteractionEvent) { billing.record("Hosted paywall control interaction") }
        }
    }
    val options = remember(state.offering, listener, close) {
        PaywallOptions.Builder(dismissRequest = close).setOffering(state.offering)
            .setShouldDisplayDismissButton(true).setListener(listener).build()
    }
    // Fullscreen SDK renderer handles introductory offers, eligibility, transactions and paywall errors.
    Paywall(options = options)
}

@Composable
fun CustomerCenterScreen(state: BillingState, billing: BillingRepository, close: () -> Unit) {
    if (state.isDemo || !state.configured) {
        Page {
            PageHeader("Customer Center", close)
            Text("Your membership,\nin one place.", style = MaterialTheme.typography.headlineLarge)
            InfoCard(if (state.premium) "Plus · demo membership" else "Free · demo membership", state.subscriptionSummary)
            InfoCard("Connect to try the real Customer Center", "The live variant opens RevenueCat’s Customer Center for subscription management, restore, support, promotional retention flows, and feedback configured in your dashboard.")
            PrimaryButton("Restore demo purchase", enabled = state.isDemo, onClick = billing::restore)
            TextButton(onClick = close) { Text("Back to my account") }
        }
        return
    }
    val options = remember(billing) {
        CustomerCenterOptions.Builder().setListener(object : CustomerCenterListener {
            override fun onRestoreStarted() { billing.record("Customer Center: restore started") }
            override fun onRestoreCompleted(customerInfo: CustomerInfo) { billing.acceptCustomerInfo(customerInfo, "Customer Center restore") }
            override fun onRestoreFailed(error: PurchasesError) { billing.record("Customer Center restore error: ${error.code}") }
            override fun onShowingManageSubscriptions() { billing.record("Customer Center: store management opened") }
            override fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) { billing.record("Customer Center: feedback submitted") }
            override fun onCustomActionSelected(actionIdentifier: String, purchaseIdentifier: String?) { billing.record("Customer Center custom action: $actionIdentifier") }
            override fun onPromotionalOfferSucceeded(customerInfo: CustomerInfo, transaction: StoreTransaction) { billing.acceptCustomerInfo(customerInfo, "Customer Center promotional offer") }
        }).build()
    }
    CustomerCenter(modifier = Modifier.fillMaxSize(), options = options, onDismiss = close)
}
