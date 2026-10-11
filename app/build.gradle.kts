import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
val rc = Properties().apply {
    val config = rootProject.file("revenuecat.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
fun configured(name: String, fallback: String = ""): String =
    providers.gradleProperty("rc.$name").orNull ?: rc.getProperty(name, fallback)
fun literal(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.example.mindfulplus"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.mindfulplus"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "RC_PUBLIC_SDK_KEY", literal(configured("publicSdkKey")))
        buildConfigField("String", "RC_ENTITLEMENT_ID", literal(configured("entitlementId", "mindful_plus")))
        buildConfigField("String", "RC_PLACEMENT_ID", literal(configured("placementId", "session_complete")))
        buildConfigField("String", "RC_PRODUCT_IDS", literal(configured("productIds")))
        buildConfigField("String", "PRIVACY_URL", literal(configured("privacyUrl")))
        buildConfigField("String", "TERMS_URL", literal(configured("termsUrl")))
    }
    flavorDimensions += "billing"
    productFlavors {
        create("demo") {
            dimension = "billing"
            applicationIdSuffix = ".demo"
            buildConfigField("boolean", "DEMO_MODE", "true")
            resValue("string", "app_name", "MindfulPlus Demo")
        }
        create("live") {
            dimension = "billing"
            buildConfigField("boolean", "DEMO_MODE", "false")
            resValue("string", "app_name", "MindfulPlus")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.revenuecat.purchases)
    implementation(libs.revenuecat.ui)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.compose.test)
}
