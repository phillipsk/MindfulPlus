package com.example.mindfulplus.billing

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package as RCPackage
import kotlinx.coroutines.flow.StateFlow

data class Plan(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val rcPackage: RCPackage? = null,
)

data class BillingState(
    val isDemo: Boolean,
    val configured: Boolean = true,
    val configurationError: String? = null,
    val busy: Boolean = false,
    val premium: Boolean = false,
    val plans: List<Plan> = emptyList(),
    val offering: Offering? = null,
    val offerings: List<String> = emptyList(),
    val metadata: String = "—",
    val userId: String = "Anonymous",
    val anonymous: Boolean = true,
    val customerInfo: CustomerInfo? = null,
    val subscriptionSummary: String = "Free membership",
    val currencies: List<String> = emptyList(),
    val products: List<String> = emptyList(),
    val notice: String? = null,
    val events: List<String> = emptyList(),
)

interface BillingRepository {
    val state: StateFlow<BillingState>
    fun refresh(force: Boolean = false)
    fun purchase(activity: Activity, plan: Plan)
    fun restore()
    fun login(userId: String)
    fun logout()
    fun saveAttributes(displayName: String, email: String, goal: String)
    fun loadOffering(id: String)
    fun lookupProducts()
    fun refreshCurrencies()
    fun manageSubscriptions(context: Context)
    fun checkPayments(context: Context)
    fun acceptCustomerInfo(info: CustomerInfo, source: String)
    fun record(event: String)
    fun clearNotice()
    fun resetDemo()
}
