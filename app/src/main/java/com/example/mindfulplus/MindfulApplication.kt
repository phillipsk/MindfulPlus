package com.example.mindfulplus

import android.app.Application
import com.example.mindfulplus.billing.*
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MindfulApplication : Application() {
    lateinit var billing: BillingRepository
        private set
    override fun onCreate() {
        super.onCreate()
        billing = if (BuildConfig.DEMO_MODE) {
            DemoBillingRepository(getSharedPreferences("demo_billing", MODE_PRIVATE))
        } else {
            var error = AccessPolicy.configurationError(BuildConfig.RC_PUBLIC_SDK_KEY, BuildConfig.RC_ENTITLEMENT_ID, BuildConfig.DEBUG)
            if (error == null) {
                try {
                    Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
                    Purchases.configure(PurchasesConfiguration.Builder(this, BuildConfig.RC_PUBLIC_SDK_KEY).build())
                } catch (e: Exception) { error = "RevenueCat setup failed: ${e.message}" }
            }
            RevenueCatBillingRepository(error)
        }
    }
}
