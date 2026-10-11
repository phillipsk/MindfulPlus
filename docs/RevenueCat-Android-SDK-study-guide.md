RevenueCat Android SDK: study guide for a prospective SDK engineer

Prepared October 11, 2026. MindfulPlus pins purchases and purchases-ui to 10.26.0. GitHub marks that release as latest at the time of this review; published documentation can evolve beyond the pinned version. [Release 10.26.0](https://github.com/RevenueCat/purchases-android/releases/tag/10.26.0).

MindfulPlus is an original fictional wellness demo created for this exercise. Its screens, name, content, local billing simulation, and navigation were written for this project. It is not a RevenueCat sample app. RevenueCat's published SDK supplies the live billing and hosted UI capabilities. Watching a demo purchase does not establish that the live integration works against a store or backend.

1. The concepts to get right first

| Concept | Meaning | MindfulPlus example |
|---|---|---|
| Store product | A purchasable item configured with the store | A real monthly or annual subscription; demo IDs are invented |
| Base plan / subscription offer | Google subscription pricing schedule and eligible purchase options | Monthly and annual may be different base plans of ONE subscription product |
| Package | RevenueCat's grouping of corresponding store products | An annual package in an offering |
| Offering | A collection of packages presented to a customer | Current offering, or one resolved for session_complete |
| Placement | A named app location that resolves an offering | session_complete |
| Entitlement | The access benefit associated with purchases | mindful_plus |
| CustomerInfo | SDK snapshot of customer subscription/access information | Drives premium access in the live repository |
| App User ID | RevenueCat customer identity | A stable ID from your own account system in a real app |

A useful flow is: authenticate in your own system if needed → configure/identify RevenueCat → resolve offering → display eligible store details → purchase → check the configured entitlement → update access. Navigation and authentication remain application responsibilities. [Products and offerings](https://www.revenuecat.com/docs/getting-started/displaying-products), [customer identity](https://www.revenuecat.com/docs/customers/identifying-customers).

2. The APIs you will use most

The names below are verified against 10.26.0 source and the app's compiled integration. Kotlin await methods are extension functions, not an independent SDK. Java integrations generally use listener/callback APIs. Older tutorials may use deprecated APIs; check the version before copying them.

| Need | Kotlin API / type | What to remember |
|---|---|---|
| Initialize | Purchases.configure(PurchasesConfiguration.Builder(context, publicKey).build()) | Configure before using sharedInstance; use the platform public SDK key |
| Observe access updates | updatedCustomerInfoListener = UpdatedCustomerInfoListener { ... } | Feed updates into app state; this is not a server push subscription |
| Read customer | awaitCustomerInfo(cacheFetchPolicy) | Know whether you asked for cached data or a current fetch |
| Invalidate customer cache | invalidateCustomerInfoCache() | Invalidating does not itself return a new snapshot |
| Read offerings | awaitOfferings() | Offering/plan availability can fail independently from customer access |
| Resolve placement | offerings.getCurrentOfferingForPlacement(id) | A null result needs an explicit product decision; this sample falls back to current |
| Select packages | offering.availablePackages | Keep the actual Package for checkout |
| Purchase | awaitPurchase(PurchaseParams.Builder(activity, rcPackage).build()) | A live Activity is needed; inspect result.customerInfo |
| Restore | awaitRestore() | A user-triggered recovery flow; success can still mean no active Plus entitlement |
| Identify | awaitLogIn(stableUserId) | Result includes CustomerInfo and created; does not authenticate the user |
| Anonymous identity | awaitLogOut() | Generates an anonymous identity; is not cancellation or account deletion |
| Direct store lookup | awaitGetProducts(productIds, ...) | Products are not the same as a remotely chosen offering |
| Availability | Purchases.awaitCanMakePayments(context) | Availability is not proof that a particular purchase will succeed |

[Configuration source](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/PurchasesConfiguration.kt), [common coroutine extensions](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/CoroutinesExtensionsCommon.kt), [customer/identity extensions](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/defaults/kotlin/com/revenuecat/purchases/coroutinesExtensions.kt).

A minimal original learning example, assuming configuration and a selected package:

```kotlin
import com.revenuecat.purchases.*

suspend fun purchasePlus(
    activity: android.app.Activity,
    selected: com.revenuecat.purchases.Package
): Boolean {
    val result = Purchases.sharedInstance.awaitPurchase(
        PurchaseParams.Builder(activity, selected).build()
    )
    return result.customerInfo.entitlements.active.containsKey("mindful_plus")
}
```

This example omits UI state, error handling, plan replacement, and lifecycle ownership. A production implementation needs those. It illustrates the key contract: derive access from the returned entitlement snapshot. Neither a click nor an apparently successful checkout is your entitlement policy.

3. CustomerInfo, caching, and access

Inspect entitlements.active[requiredId], then the fields that explain the state: productIdentifier, productPlanIdentifier, expirationDate, willRenew, periodType, store, isSandbox, and billingIssueDetectedAt. Cancellation of renewal does not necessarily mean current access has ended. An empty entitlement map can be a valid result. The customer listener delivers SDK-observed changes; proactively fetching remains necessary when you need updated information. [CustomerInfo documentation](https://www.revenuecat.com/docs/customers/customer-info).

The four cache policies in the pinned SDK:

| Policy | Behavior |
|---|---|
| CACHE_ONLY | Read cache; fail if unavailable; no fetch |
| FETCH_CURRENT | Fetch current data; fail if the fetch fails |
| NOT_STALE_CACHED_OR_CURRENT | Use a fresh cache or fetch; does not return stale data when the fetch fails |
| CACHED_OR_FETCHED | Default: return cached data even if stale; if stale, refresh in the background; fetch if no cache exists |

[CacheFetchPolicy source](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/CacheFetchPolicy.kt), [caching documentation](https://www.revenuecat.com/docs/test-and-launch/debugging/caching).

Design questions worth practicing: What does the UI show while the first snapshot loads? Does a network error preserve already-established access? What happens if an old customer's response arrives after an identity switch? Which screen requires freshness, and which can use cached state? A Boolean alone loses the distinction between loading, known inactive, active, stale, and failed. These are useful app-level design considerations, not promises of additional SDK states.

4. Purchases, pending payments, cancellation, and replacement

Know PurchasesException, PurchasesTransactionException.userCancelled, PurchasesErrorCode, and PaymentPendingError. Treat user cancellation as an ordinary outcome. Pending payment must not grant access prematurely. Log diagnostic codes, preserve retryable context, and avoid retrying checkout blindly. The coroutine API also has awaitPurchaseResult/awaitRestoreResult variants in 10.26.0; choose one error-handling style deliberately. [Making purchases](https://www.revenuecat.com/docs/getting-started/making-purchases), [common coroutine source](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/CoroutinesExtensionsCommon.kt).

PurchaseParams.Builder accepts a Package, StoreProduct, or SubscriptionOption. A package/product uses its default subscription option; a specific option makes offer selection explicit. Use store-provided formatted prices and understand introductory phases. For an existing Play subscription, oldProductId(...) and replacementMode(StoreReplacementMode...) carry replacement intent. The older googleReplacementMode setter is deprecated. MindfulPlus uses WITHOUT_PRORATION as a sample choice; your product policy must choose appropriate timing/proration. Same product ID with a different base plan is still a meaningful change. [PurchaseParams source](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/PurchaseParams.kt), [subscription management](https://www.revenuecat.com/docs/subscription-guidance/managing-subscriptions).

For SDK interviews, be able to explain the boundary with Google Play: purchase discovery, connection recovery, pending versus purchased state, token verification, acknowledgment/consumption ownership, and backend reconciliation. RevenueCat normally owns the billing flow in its standard integration; adding a second independent BillingClient implementation can create competing ownership. Google's integration documentation is useful for understanding what an SDK has to handle beneath its public API. [Google Billing integration](https://developer.android.com/google/play/billing/integrate).

5. Restore and identity are different operations

awaitRestore is the visible user recovery action. awaitSyncPurchases supports deliberate synchronization/migration cases and integrations that own purchase completion. It should not casually replace a Restore button. Understand purchasesAreCompletedBy(REVENUECAT or MY_APP) before integrating around an existing checkout implementation. Deprecated observerMode examples can obscure that ownership choice. [Restore guidance](https://www.revenuecat.com/docs/getting-started/restoring-purchases), [configuration source](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/PurchasesConfiguration.kt).

Use a stable, case-sensitive account identifier from your authenticated backend. Study anonymous → identified, identified → another identified, logout → new anonymous, reinstall, and a purchase restored under another account. Aliasing and transfer behavior depend on RevenueCat project settings. Never hard-code one customer ID for everyone. Subscriber attributes are not authentication or access grants. [Identifying customers](https://www.revenuecat.com/docs/customers/identifying-customers).

A strong interview discussion separates three identifiers: your authenticated account, RevenueCat's customer/aliases, and the store account/token. They are not interchangeable. Explain how your UI avoids showing a previous account's state during a switch, and how support can diagnose a restore without exposing sensitive identifiers unnecessarily.

6. Paywalls, Customer Center, attributes, and currency

| Feature | APIs in this app | Practical boundary |
|---|---|---|
| Hosted Compose paywall | Paywall, PaywallOptions.Builder(dismissRequest = ...), setOffering, setListener, setShouldDisplayDismissButton | Requires a configured SDK and published offering/paywall |
| Other paywall entry points | PaywallDialog, PaywallDialogOptions, PaywallActivityLauncher | Useful alternatives for Compose/Activity integration; not added to this sample |
| Paywall events | PaywallListener purchase/restore start, completion, error, cancellation; URL, interaction, web checkout callbacks | Update access from CustomerInfo; events alone do not grant access |
| Customer Center | CustomerCenter, CustomerCenterOptions, CustomerCenterListener | Dashboard configuration and store capabilities determine available flows |
| Subscription management | awaitShowManageSubscriptions(context); CustomerInfo.managementURL | Manage the subscription on its originating platform |
| Subscriber attributes | setDisplayName, setEmail, setAttributes | Values may be sent to RevenueCat; do not put secrets in custom attributes |
| Attribute targeting | awaitSyncAttributesAndOfferingsIfNeeded() | Set attributes before resolving targeting; synchronization is rate limited |
| Virtual currency | awaitGetVirtualCurrencies(), invalidateVirtualCurrenciesCache() | Reading balances is separate from spending/granting currency |

[Android paywall display](https://www.revenuecat.com/docs/tools/paywalls/displaying-paywalls), [Android Customer Center](https://www.revenuecat.com/docs/tools/customer-center/customer-center-integration-android), [subscriber attributes](https://www.revenuecat.com/docs/customers/customer-attributes), [virtual currencies](https://www.revenuecat.com/docs/offerings/virtual-currency).

MindfulPlus's demo Customer Center and hosted setup page are explanatory placeholders. They do not run the actual SDK-rendered flows. The live source wires the real composables/listeners, but this recording session has no public key. Demo identity, balances, products, plan changes, and restores are local examples. In particular, selecting demo_focus only logs a selection; it does not fetch targeted packages.

CustomerCenterListener exposes restore start/completion/failure, subscription-management, feedback, custom-action, and promotional-offer callbacks. Its onRestoreInitiated(resume) hook can pause restore for an application precondition such as authentication; the listener must resume or decline it. That hook is not implemented in this sample. [Customer Center listener source](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/customercenter/CustomerCenterListener.kt).

7. Security and server boundaries

A public Android SDK key belongs in the client configuration; secret RevenueCat REST credentials do not. Configure production app identity and package/store details intentionally. Trusted Entitlements supplies signature verification results; in modern Android versions of this SDK it is informational by default, and the application decides its response to FAILED or unverified results. MindfulPlus does not implement a custom verification-result policy. Signing a response does not make arbitrary client code a secure backend. Protect server resources and economic currency operations with authenticated server authorization. [Android installation](https://www.revenuecat.com/docs/getting-started/installation/android), [Trusted Entitlements](https://www.revenuecat.com/docs/customers/trusted-entitlements).

You should recognize server notifications, webhooks, retries, deduplication, and eventual consistency even if your main work is Kotlin. CustomerInfo, the Play subscription resource, your service's authorization state, and analytics may update at different times. Do not assume a callback is an exactly-once distributed transaction. This is a system-design concern to reason through; the demo has no backend or webhook implementation.

8. How to read the SDK repository

Read these at the 10.26.0 tag so implementation and your demo align. Internal classes below are study targets, not APIs your app should call.

| Area | Starting files / directories | Question to answer |
|---|---|---|
| Public façade | purchases/.../Purchases.kt, PurchasesConfiguration.kt | What is the stable consumer contract? |
| Async adapters | CoroutinesExtensionsCommon.kt; defaults/.../coroutinesExtensions.kt | How do callbacks map to suspension, exceptions, and cancellation? |
| Coordination | purchases/.../PurchasesOrchestrator.kt | How do checkout, receipt posting, customer updates, and recovery connect? |
| Play adapter | purchases/.../google/BillingWrapper.kt | How are store connection and purchase results handled? |
| Identity | purchases/.../identity/IdentityManager.kt | What must be cleared or fetched during a switch? |
| Persistence | purchases/.../common/caching/DeviceCache.kt | Which values are keyed by customer and how does staleness work? |
| UI | ui/revenuecatui | How does remote configuration become Compose UI and events? |
| Compatibility | api-tester; scripts/api-dump.sh | How are Java/Kotlin usage and public API changes checked? |
| Test layers | purchases tests, integration-tests, maestro, test-apps | Which layer can reproduce a reported bug? |
| Other stores | feature/galaxy; store adapters | Which logic is shared and which is platform-specific? |

[Repository tree](https://github.com/RevenueCat/purchases-android/tree/10.26.0), [orchestrator](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/PurchasesOrchestrator.kt), [BillingWrapper](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/google/BillingWrapper.kt), [IdentityManager](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/identity/IdentityManager.kt), [DeviceCache](https://github.com/RevenueCat/purchases-android/blob/10.26.0/purchases/src/main/kotlin/com/revenuecat/purchases/common/caching/DeviceCache.kt).

A cancellable coroutine does not imply that cancelling a screen reverses a store transaction. Review what happens to late callbacks and state reconciliation. Likewise, an SDK is a library inside someone else's app: it should avoid retaining Activities, blocking the main thread, owning unnecessary global state, or adding surprising startup work. These are design questions for source reading, not allegations about the SDK.

9. What the videos do and do not prove

The recorded assertions verify app-level journeys in a separate build with unchanged production screens. They do not exercise Google's real purchase sheet, real renewal, refund, pending payment recovery, remote targeting, live identity transfers, or server-backed balances.

For an actual integration, progress in layers: local demo → RevenueCat Test Store with a test_ key → Google Play license-tester/internal-track testing → production monitoring. Test Store uses the SDK/backend and updates actual RevenueCat CustomerInfo, unlike this app's demo. It still cannot prove Play-specific billing behavior. Keep test credentials out of a shipped production build. [Test Store](https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store).

A useful real-store test matrix includes: initial purchase; user cancellation; pending completion/cancellation; renewal; cancel-but-unexpired; grace period; account hold; expiry; recovery; same-product base-plan switch; different-product switch; restore after reinstall; account switch during in-flight fetch; offline launch; service disconnect; dashboard product mismatch; and hosted UI configuration failures. Google's subscription lifecycle describes grace-period access, account-hold loss of access, and cancellation retaining access through the paid period. [Subscription lifecycle](https://developer.android.com/google/play/billing/lifecycle/subscriptions).

10. Review the sample as an SDK engineer

These are source-review observations and proposed hardening work, not newly implemented fixes:

- The live repository refreshes customer information and offerings during initialization from Application startup. RevenueCat advises against explicitly pre-warming offerings in Application.onCreate. Move offering demand to an appropriate visible flow while retaining safe customer initialization. [Offering guidance](https://www.revenuecat.com/docs/getting-started/displaying-products).
- Login accepts the new CustomerInfo before fetching offerings, but clears displayed currencies/products only AFTER that fetch. If offering fetch fails, previous-account auxiliary data may remain. Clear identity-scoped presentation state at the identity transition, and test partial failure.
- Ordinary refresh resolves the configured placement, while login/logout/attribute sync selects current directly. Decide whether that inconsistency is intended; use one selection policy if the placement should remain authoritative.
- The local demo stores only a premium Boolean. It does not model expiry, account ownership, or durable plan details; an app process restart can show access without the former plan summary. A richer fake should model the contract it claims to demonstrate.
- A currency-fetch error after purchase can overwrite the success notice even when Plus has unlocked. Preserve the purchase outcome and show auxiliary refresh failures separately.
- The snackbar effect is keyed by billing.notice and clears that same notice before awaiting showSnackbar. That state change can cancel the effect that owns the snackbar. Review transient event delivery separately; video captions explain actions even when the notice is brief.
- The live-unconfigured Customer Center fallback currently reuses a "demo membership" label. Make that message distinguish missing configuration from actual demo mode.
- MainActivity already refreshes customer state on lifecycle resume. The sample still does not model loading versus inactive access in depth or apply a custom signature-verification policy; those are review topics before production.

The point of the exercise is to learn the public contract and practice finding gaps between a pleasant demo and a robust integration. A small reproducible bug, a clear contract decision, and a meaningful regression test would be a stronger SDK portfolio addition than adding several more happy-path screens.
