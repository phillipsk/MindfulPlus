package com.example.mindfulplus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mindfulplus.BuildConfig
import com.example.mindfulplus.JourneyState
import com.example.mindfulplus.billing.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SdkLabScreen(journey: JourneyState, state: BillingState, billing: BillingRepository, back: () -> Unit, reset: () -> Unit) {
    val context = LocalContext.current
    var userId by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmIdentity by rememberSaveable { mutableStateOf(false) }
    Page {
        PageHeader("RevenueCat SDK lab", back)
        Text("See what’s\nhappening.", style = MaterialTheme.typography.headlineLarge)
        ModeBadge(state.isDemo)
        SelectionContainer {
            InfoCard("SDK 10.26.0 · ${BuildConfig.FLAVOR}", "Entitlement: ${BuildConfig.RC_ENTITLEMENT_ID}\nPlacement: ${BuildConfig.RC_PLACEMENT_ID}\nPlus: ${state.premium}\nUser: ${state.userId}\nAnonymous: ${state.anonymous}\nOffering: ${state.offering?.identifier ?: if (state.isDemo) "demo_default" else "none"}")
        }
        state.configurationError?.let { InfoCard("Configuration needed", it) }
        PrimaryButton("Force refresh customer info", enabled = state.configured && !state.busy) { billing.refresh(force = true) }
        OutlinedButton(onClick = { billing.checkPayments(context) }, enabled = state.configured && !state.busy) { Text("Check store billing availability") }
        Text("Offerings & targeting", style = MaterialTheme.typography.titleLarge)
        Text("The first load resolves the configured placement, with the current offering as a fallback. Choose another offering to inspect its packages.", color = Muted, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.offerings.forEach { id -> AssistChip(onClick = { billing.loadOffering(id) }, label = { Text(id) }, enabled = !state.busy) }
        }
        InfoCard("Offering metadata", state.metadata)
        Text("Customer identity", style = MaterialTheme.typography.titleLarge)
        Text("This is a developer identity switch, not sign-in. In a real app, use the stable ID from your authenticated account. RevenueCat may alias or transfer purchases according to your project settings.", style = MaterialTheme.typography.bodySmall, color = Muted)
        OutlinedTextField(userId, { userId = it }, label = { Text("App user ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { confirmIdentity = true }, enabled = state.configured && !state.busy && userId.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Identify this customer") }
        TextButton(onClick = billing::logout, enabled = state.configured && !state.busy && !state.anonymous) { Text("Log out to anonymous identity") }
        Text("Subscriber attributes", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it }, label = { Text("Display name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Sending attributes shares these values and your selected goal with RevenueCat. Leave them blank to clear saved name/email.", style = MaterialTheme.typography.bodySmall, color = Muted)
        OutlinedButton(onClick = { billing.saveAttributes(name, email, journey.goal) }, enabled = state.configured && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Send attributes & sync offerings") }
        Text("Virtual currencies", style = MaterialTheme.typography.titleLarge)
        Text(state.currencies.joinToString("\n").ifEmpty { "No balances loaded. Configure currencies in RevenueCat, then refresh." }, color = Muted)
        OutlinedButton(onClick = billing::refreshCurrencies, enabled = state.configured && !state.busy) { Text("Refresh currency balances") }
        Text("Store product lookup", style = MaterialTheme.typography.titleLarge)
        Text(state.products.joinToString("\n").ifEmpty { "Set productIds in revenuecat.properties for direct SDK product lookup." }, color = Muted)
        OutlinedButton(onClick = billing::lookupProducts, enabled = state.configured && !state.busy) { Text("Fetch configured product IDs") }
        state.customerInfo?.let { info ->
            SelectionContainer {
                InfoCard("Customer snapshot", "Active entitlements: ${info.entitlements.active.keys.joinToString().ifEmpty { "none" }}\nActive subscriptions: ${info.activeSubscriptions.joinToString().ifEmpty { "none" }}\nPurchased products: ${info.allPurchasedProductIds.joinToString().ifEmpty { "none" }}\nLast response: ${info.requestDate}\nSubscription management link: ${if (info.managementURL != null) "available" else "none"}")
            }
        }
        Text("Recent events", style = MaterialTheme.typography.titleLarge)
        SelectionContainer { Text(state.events.joinToString("\n\n"), style = MaterialTheme.typography.bodySmall, color = Muted) }
        if (state.isDemo) {
            OutlinedButton(onClick = billing::resetDemo, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Reset demo Plus access") }
            TextButton(onClick = { confirmReset = true }) { Text("Restart onboarding & clear demo progress") }
        }
    }
    if (confirmIdentity) AlertDialog(onDismissRequest = { confirmIdentity = false }, title = { Text("Change RevenueCat identity?") },
        text = { Text("This identifies the customer as “$userId”. It does not authenticate them. Purchase aliasing and transfer settings may affect their entitlements.") },
        confirmButton = { TextButton(onClick = { confirmIdentity = false; billing.login(userId) }) { Text("Identify customer") } },
        dismissButton = { TextButton(onClick = { confirmIdentity = false }) { Text("Cancel") } })
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Restart the demo?") },
        text = { Text("This clears local demo progress, onboarding, and simulated Plus access.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; reset() }) { Text("Restart") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}
