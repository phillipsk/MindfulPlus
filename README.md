# MindfulPlus

A runnable Android wellness sample built with Kotlin, Jetpack Compose, Material 3, Navigation Compose, and Gradle Kotlin DSL. RevenueCat Purchases and Purchases UI are pinned to **10.26.0**, verified against the October 8, 2026 GitHub release.

This original, AI-assisted learning sample was created from scratch; it is not a RevenueCat sample app. Demo billing is local simulation. Real SDK/store transactions require your own project configuration.

## Run it now

Open the repository directory in Android Studio. Select the **demoDebug** build variant and run `app`. It needs no RevenueCat account or API key. The application ID is `com.example.mindfulplus.demo`, so demo and live can coexist.

Walkthrough:

1. Tap **Find my rhythm**, choose an intention, and complete onboarding.
2. On Today, open **Morning reset**, advance through its three guided text steps, and finish the session.
3. The Plus paywall appears. **Maybe later** keeps free content usable. **Simulate Plus purchase** grants local demo access with no store transaction or charge.
4. Explore includes two free previews and two premium previews. Opening a locked preview offers Plus; unlocking returns you to the requested preview.
5. Progress persists the number of completed previews on this device.
6. You includes membership details, restore, Customer Center, store management, and the SDK lab.
7. In the SDK lab, reset demo access or restart the complete journey.

These are text previews with original Compose vector artwork. The sample has no audio playback, clinical guidance, backend authentication, or remote content service.

## Connect actual RevenueCat billing

Copy `revenuecat.properties.example` to the ignored `revenuecat.properties` file in the project root. Populate it with a **public Android SDK key** and your exact entitlement ID. Never use a secret `sk_` API key.

```properties
publicSdkKey=test_YOUR_PUBLIC_TEST_STORE_KEY
entitlementId=mindful_plus
placementId=session_complete
productIds=
privacyUrl=
termsUrl=
```

Then select **liveDebug**, sync Gradle, and run the app. The live application ID is `com.example.mindfulplus`; configure that package in Google Play if testing Google Play billing. To use your own package name, update `applicationId` in `app/build.gradle.kts` before registering store products.

For a first SDK test, use RevenueCat Test Store:

1. Create/select a RevenueCat project and its Test Store under Apps and providers.
2. Add monthly and annual Test Store products. Attach them to the `mindful_plus` entitlement (or the entitlement ID you configured).
3. Create an offering, add monthly/annual packages, and mark it current. Optionally configure the `session_complete` placement.
4. Copy that Test Store’s public `test_` key into the file above and run **liveDebug**. The actual SDK invokes the Test Store purchase modal and updates RevenueCat customer state.
5. Publish a paywall on the offering to try **Show RevenueCat paywall**. Configure Customer Center separately to try its real self-service flow.

Test Store uses simulated store outcomes backed by RevenueCat; the **demo** flavor is an entirely local simulation. Missing/invalid live configuration shows setup instructions and disables checkout. Live mode never substitutes demo prices or demo access.

For Google Play, use your public `goog_` key, connect Google Play to RevenueCat, create subscription products/base plans and packages, and test with the matching package and a configured license tester. Follow the official Google Play sandbox guide for installation and tester setup. Real store checkout can charge a real customer outside the sandbox.

Before distributing a real app, provide your app’s published privacy/terms URLs, your own package/branding, release signing, and authenticated account IDs. The live release path refuses to initialize Test Store keys. No store account, products, dashboard paywall, account credentials, or release-signing key is supplied in this project.

## SDK features and entry points

| Feature | Where to try it | Implementation |
| --- | --- | --- |
| Configure SDK once; public-key validation; debug logging | liveDebug launch | `MindfulApplication` |
| Offerings, packages, localized store prices | Custom Plus paywall | `awaitOfferings`, `PurchaseParams.Builder`, `awaitPurchase` |
| Placement targeting and current-offering fallback | First offerings load | `getCurrentOfferingForPlacement` |
| Offering inspection and metadata | SDK lab | All offerings and available packages |
| Fullscreen RevenueCat Compose paywall | Show RevenueCat paywall | `Paywall`, `PaywallOptions`, `PaywallListener` |
| Purchase, cancellation, pending payment, error handling | Store checkout | Coroutine transaction results and typed errors |
| Entitlement-gated content and return after upgrade | Premium library sessions | Exact configured entitlement from `CustomerInfo` |
| Automatic customer updates and foreground refresh | Membership screen | `UpdatedCustomerInfoListener`, lifecycle resume |
| Restore without assuming access was granted | Paywall or account | `awaitRestore` and fresh entitlement check |
| Google Play plan replacement, including same-product base plans | Membership plans | Old product ID, `WITHOUT_PRORATION` replacement mode |
| Anonymous and identified customers, log in/out | SDK lab | `awaitLogIn`, `awaitLogOut`; this is not authentication |
| Name, email, goal attributes and targeting sync | SDK lab | `setDisplayName`, `setEmail`, `setAttributes`, attribute/offering sync |
| Direct store product lookup | SDK lab | `awaitGetProducts` using configured product IDs |
| Store billing availability | SDK lab | `awaitCanMakePayments` |
| Entitlements, subscriptions, expiry, renewal, billing issue, sandbox status | Account and SDK lab | `CustomerInfo`, `EntitlementInfo` |
| Virtual currency balances and cache invalidation | SDK lab | `awaitGetVirtualCurrencies`, currency cache refresh |
| Customer Center and restore/promotional/feedback/custom-action events | You → Customer Center | SDK Compose Customer Center and listener |
| Store subscription management | You | `awaitShowManageSubscriptions` |
| Paywall purchase/restore/control/link events | SDK lab event history | SDK listener callbacks |
| Forced customer refresh/cache invalidation | SDK lab | Fetch current policy and cache invalidation |

The custom paywall purchases the package’s default eligible subscription option; final introductory/trial terms are shown by checkout. The app does not promise a free trial. Hosted paywall behavior and retention promotions follow your RevenueCat configuration. Currency spending, webhooks, analytics dashboards, refunds, server integrations, and store-specific Amazon/Galaxy modules require separate configuration or backend work and are not implemented here.

## Build and test

Toolchain: Gradle 8.13, AGP 8.11.2, Kotlin/Compose compiler 2.3.10, Compose BOM 2025.08.01, compile/target SDK 36, minimum SDK 24, Java target 17. Android Studio’s bundled JBR 21 was used for validation. These compatible versions are intentionally pinned; RevenueCat is the latest release found when this sample was created. The official wrapper includes the distribution SHA-256 checksum.

```sh
./gradlew assembleDemoDebug assembleLiveDebug
./gradlew testDemoDebugUnitTest testLiveDebugUnitTest lintDemoDebug lintLiveDebug
./gradlew connectedDemoDebugAndroidTest connectedLiveDebugAndroidTest
```

The demo journey test checks onboarding, an automatic paywall after a free session, dismissal, a premium-content gate, simulated purchase, revocation while premium content is open, restore, progress, and a fresh Activity launch. The live setup test runs when the live key is blank and confirms checkout is disabled while free content remains available. Each flavor skips the other flavor’s instrumentation case. The tests also export demo PNG previews to the device’s external cache.

Unit coverage includes exact-entitlement matching, no-entitlement access, upgrade timing, public/secret key rules, debug-only Test Store keys, missing configuration, and product/base-plan replacement decisions.

See [the Android SDK study guide](docs/RevenueCat-Android-SDK-study-guide.md) for API contracts, source-reading pointers, and verification limits. SDK-backed checkout, hosted paywalls, and Customer Center require a configured key/dashboard and were not transaction-tested without one.

## Source map

- `billing/RevenueCatBillingRepository.kt`: real SDK calls, customer state, concurrency/error handling.
- `billing/DemoBillingRepository.kt`: isolated local simulation.
- `billing/AccessPolicy.kt`: access/configuration/replacement rules.
- `AppViewModel.kt`: local onboarding and practice progress.
- `ui/MindfulApp.kt`: navigation, premium gates, return after purchase.
- `ui/BillingScreens.kt`: custom/hosted paywalls and Customer Center.
- `ui/SdkLabScreen.kt`: SDK exploration tools.

## Official references checked

- [RevenueCat Android SDK on GitHub](https://github.com/RevenueCat/purchases-android)
- [SDK 10.26.0 release](https://github.com/RevenueCat/purchases-android/releases/tag/10.26.0)
- [Android installation](https://www.revenuecat.com/docs/getting-started/installation/android)
- [Displaying paywalls](https://www.revenuecat.com/docs/tools/paywalls/displaying-paywalls)
- [Customer Center for Android](https://www.revenuecat.com/docs/tools/customer-center/customer-center-integration-android)
- [RevenueCat Test Store](https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store)
- [Google Play sandbox](https://www.revenuecat.com/docs/test-and-launch/sandbox/google-play)
- [Subscription management](https://www.revenuecat.com/docs/subscription-guidance/managing-subscriptions)
- [Virtual currencies](https://www.revenuecat.com/docs/offerings/virtual-currency)
- [AGP 8.11 compatibility](https://developer.android.com/build/releases/agp-8-11-0-release-notes)
