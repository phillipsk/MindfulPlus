package com.example.mindfulplus.billing

import android.app.Activity
import android.content.Context
import com.example.mindfulplus.BuildConfig
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.DateFormat

class RevenueCatBillingRepository(configurationError: String?) : BillingRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(BillingState(
        isDemo = false, configured = configurationError == null, configurationError = configurationError,
    ))
    override val state = mutable.asStateFlow()
    private var offerings: Offerings? = null
    private val sdk get() = Purchases.sharedInstance

    init {
        if (configurationError == null) {
            sdk.updatedCustomerInfoListener = UpdatedCustomerInfoListener { acceptCustomerInfo(it, "SDK listener") }
            refresh()
        }
    }

    private fun operation(label: String, block: suspend () -> Unit) {
        if (!state.value.configured || state.value.busy) return
        scope.launch {
            mutable.update { it.copy(busy = true, notice = null) }
            record("$label started")
            try { block() } catch (error: CancellationException) { throw error }
            catch (error: Exception) { report(error, label) }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }

    private fun report(error: Exception, operation: String) {
        val message = when {
            error is PurchasesTransactionException && error.userCancelled -> "Purchase canceled. You have not been charged by this app."
            error is PurchasesException && error.code == PurchasesErrorCode.PaymentPendingError -> "Payment is pending. Plus will unlock when the store confirms it."
            else -> "$operation: ${error.message ?: "Unable to complete this request. Try again."}"
        }
        mutable.update { it.copy(notice = message) }
        record(message)
    }

    override fun refresh(force: Boolean) = operation("Refresh") {
        if (force) sdk.invalidateCustomerInfoCache()
        // Fetch independently so an offerings/configuration failure does not hide membership state.
        try { acceptCustomerInfo(sdk.awaitCustomerInfo(if (force) CacheFetchPolicy.FETCH_CURRENT else CacheFetchPolicy.CACHED_OR_FETCHED), "Refresh") }
        catch (e: CancellationException) { throw e } catch (e: Exception) { report(e, "Customer info") }
        try {
            offerings = sdk.awaitOfferings()
            val selected = if (BuildConfig.RC_PLACEMENT_ID.isNotBlank()) {
                offerings?.getCurrentOfferingForPlacement(BuildConfig.RC_PLACEMENT_ID) ?: offerings?.current
            } else offerings?.current
            showOffering(selected)
        } catch (e: CancellationException) { throw e } catch (e: Exception) { report(e, "Offerings") }
    }

    private fun showOffering(offering: Offering?) {
        val plans = offering?.availablePackages.orEmpty().map {
            Plan(it.identifier, when (it.packageType) {
                PackageType.ANNUAL -> "Annual"
                PackageType.MONTHLY -> "Monthly"
                PackageType.WEEKLY -> "Weekly"
                PackageType.LIFETIME -> "Lifetime"
                else -> it.product.title
            }, it.product.price.formatted, it.product.period?.iso8601 ?: "One-time purchase", it)
        }
        mutable.update { it.copy(
            plans = plans, offering = offering, offerings = offerings?.all?.keys?.sorted().orEmpty(),
            metadata = offering?.metadata?.entries?.joinToString { "${it.key}: ${it.value}" }?.ifEmpty { "No metadata" } ?: "No offering",
        ) }
        record("Offering ${offering?.identifier ?: "none"}: ${plans.size} packages")
    }

    override fun purchase(activity: Activity, plan: Plan) = operation("Purchase") {
        val rcPackage = requireNotNull(plan.rcPackage) { "No store package selected" }
        val builder = PurchaseParams.Builder(activity, rcPackage)
        val active = state.value.customerInfo?.entitlements?.active?.get(BuildConfig.RC_ENTITLEMENT_ID)
        // Cover both different subscription products and monthly/annual base plans of the same product.
        val newBasePlan = (rcPackage.product.defaultOption as? GoogleSubscriptionOption)?.basePlanId
        if (active?.store == Store.PLAY_STORE && rcPackage.product.type == ProductType.SUBS &&
            AccessPolicy.shouldReplaceSubscription(active.productIdentifier, active.productPlanIdentifier, rcPackage.product.id, newBasePlan)) {
            builder.oldProductId(active.productIdentifier).replacementMode(StoreReplacementMode.WITHOUT_PRORATION)
        }
        val result = sdk.awaitPurchase(builder.build())
        acceptCustomerInfo(result.customerInfo, "Purchase")
        mutable.update { it.copy(notice = if (it.premium) "Plus is ready. Enjoy your next session." else "Purchase received, but the configured Plus entitlement is not active. Check its product mapping.") }
        refreshCurrencyData()
    }

    override fun restore() = operation("Restore purchases") {
        acceptCustomerInfo(sdk.awaitRestore(), "Restore")
        mutable.update { it.copy(notice = if (it.premium) "Your Plus membership is restored." else "Restore completed. No active Plus entitlement was found for this account.") }
    }

    override fun login(userId: String) {
        if (userId.isBlank()) { mutable.update { it.copy(notice = "Enter an app user ID first.") }; return }
        operation("Identify customer") {
            val result = sdk.awaitLogIn(userId.trim())
            acceptCustomerInfo(result.customerInfo, "Login")
            offerings = sdk.awaitOfferings()
            showOffering(offerings?.current)
            mutable.update { it.copy(currencies = emptyList(), products = emptyList(), notice = "RevenueCat identity updated. This does not authenticate an account.") }
        }
    }

    override fun logout() = operation("Reset identity") {
        if (sdk.isAnonymous) { mutable.update { it.copy(notice = "You already have an anonymous identity.") }; return@operation }
        acceptCustomerInfo(sdk.awaitLogOut(), "Logout")
        mutable.update { it.copy(plans = emptyList(), offering = null, offerings = emptyList(), currencies = emptyList(), products = emptyList()) }
        offerings = sdk.awaitOfferings(); showOffering(offerings?.current)
    }

    override fun saveAttributes(displayName: String, email: String, goal: String) = operation("Subscriber attributes") {
        sdk.setDisplayName(displayName.trim().ifBlank { null })
        sdk.setEmail(email.trim().ifBlank { null })
        sdk.setAttributes(mapOf("wellness_goal" to goal, "demo_app" to "mindfulplus"))
        offerings = sdk.awaitSyncAttributesAndOfferingsIfNeeded()
        showOffering(offerings?.current)
        mutable.update { it.copy(notice = "Attributes queued and offerings synchronized. The SDK rate-limits attribute sync.") }
    }

    override fun loadOffering(id: String) {
        showOffering(offerings?.all?.get(id))
    }

    override fun lookupProducts() = operation("Product lookup") {
        val ids = BuildConfig.RC_PRODUCT_IDS.split(',').map(String::trim).filter(String::isNotBlank)
        require(ids.isNotEmpty()) { "Set productIds in revenuecat.properties to use direct product lookup." }
        val products = sdk.awaitGetProducts(ids)
        mutable.update { it.copy(products = products.map { p -> "${p.id} · ${p.price.formatted} · ${p.type}" }, notice = "${products.size} store products found.") }
    }

    override fun refreshCurrencies() = operation("Currency balances") { sdk.invalidateVirtualCurrenciesCache(); refreshCurrencyData() }
    private suspend fun refreshCurrencyData() {
        try {
            val values = sdk.awaitGetVirtualCurrencies()
            mutable.update { it.copy(currencies = values.all.values.map { v -> "${v.name} (${v.code}) · ${v.balance}" }) }
            record("Currency balances loaded: ${values.all.size}")
        } catch (e: CancellationException) { throw e } catch (e: Exception) { report(e, "Currency balances") }
    }
    override fun manageSubscriptions(context: Context) = operation("Manage subscriptions") { sdk.awaitShowManageSubscriptions(context) }
    override fun checkPayments(context: Context) = operation("Billing availability") {
        val available = Purchases.awaitCanMakePayments(context)
        mutable.update { it.copy(notice = if (available) "Store billing is available." else "Store billing is unavailable on this device.") }
        record("Can make payments: $available")
    }

    override fun acceptCustomerInfo(info: CustomerInfo, source: String) {
        val entitlement = info.entitlements.active[BuildConfig.RC_ENTITLEMENT_ID]
        val expiration = entitlement?.expirationDate?.let { DateFormat.getDateInstance().format(it) } ?: "No expiry"
        mutable.update { it.copy(
            customerInfo = info,
            premium = AccessPolicy.hasAccess(info.entitlements.active.keys, BuildConfig.RC_ENTITLEMENT_ID),
            userId = sdk.appUserID, anonymous = sdk.isAnonymous,
            subscriptionSummary = entitlement?.let { e ->
                "${e.productIdentifier}${e.productPlanIdentifier?.let { " · $it" } ?: ""}\n${if (e.willRenew) "Renews" else "Access ends"}: $expiration\n${e.periodType} · ${e.store}${if (e.isSandbox) " · sandbox" else ""}${if (e.billingIssueDetectedAt != null) "\nBilling issue detected" else ""}"
            } ?: "Free membership",
        ) }
        record("$source: Plus entitlement ${if (state.value.premium) "active" else "inactive"}")
    }
    override fun record(event: String) { mutable.update { it.copy(events = (listOf(event) + it.events).take(40)) } }
    override fun clearNotice() { mutable.update { it.copy(notice = null) } }
    override fun resetDemo() = Unit
}
