package com.example.mindfulplus.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.mindfulplus.*
import kotlinx.coroutines.launch

@Composable
fun MindfulApp(vm: AppViewModel = viewModel()) {
    val billing by vm.billing.state.collectAsStateWithLifecycle()
    val journey by vm.journey.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snack = remember { SnackbarHostState() }
    val activity = requireNotNull(LocalActivity.current)
    var pendingSession by rememberSaveable { mutableStateOf("") }
    var wasPremiumAtPaywall by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun upgrade(sessionId: String = "") {
        pendingSession = sessionId
        wasPremiumAtPaywall = billing.premium
        nav.navigate("paywall") { launchSingleTop = true }
    }
    fun openSession(id: String) {
        val session = sessions.first { it.id == id }
        if (session.premium && !billing.premium) upgrade(id) else nav.navigate("session/$id")
    }
    fun dismissPaywall() { pendingSession = ""; nav.popBackStack() }
    LaunchedEffect(billing.notice) {
        billing.notice?.let { message ->
            vm.billing.clearNotice()
            snack.showSnackbar(message, duration = SnackbarDuration.Long)
        }
    }
    LaunchedEffect(billing.premium, route) {
        if (billing.premium && !wasPremiumAtPaywall && route in listOf("paywall", "hosted")) {
            val next = pendingSession
            pendingSession = ""
            nav.popBackStack("paywall", inclusive = true)
            if (next.isNotBlank()) nav.navigate("session/$next") { launchSingleTop = true }
        }
    }
    BackHandler(enabled = route == "paywall") { dismissPaywall() }
    val tabs = listOf("home", "library", "progress", "profile")
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (route in tabs) NavigationBar(containerColor = Cream) {
                listOf(Triple("home", "Today", Icons.Outlined.WbSunny), Triple("library", "Explore", Icons.Outlined.Spa),
                    Triple("progress", "Progress", Icons.Outlined.Insights), Triple("profile", "You", Icons.Outlined.PersonOutline)).forEach { (target, title, icon) ->
                    NavigationBarItem(selected = route == target, onClick = {
                        nav.navigate(target) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, icon = { Icon(icon, contentDescription = null) }, label = { Text(title) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = if (journey.onboarded) "home" else "welcome") {
                composable("welcome") { WelcomeScreen(billing.isDemo) { nav.navigate("goal") } }
                composable("goal") { GoalScreen(journey.goal, vm::chooseGoal, { nav.popBackStack() }) { nav.navigate("plan") } }
                composable("plan") { PlanScreen(journey.goal, { nav.popBackStack() }) {
                    vm.finishOnboarding()
                    nav.navigate("home") { popUpTo("welcome") { inclusive = true } }
                } }
                composable("home") { HomeScreen(journey, billing, { openSession("morning") }, { nav.navigate("library") }, { upgrade() }) }
                composable("library") { LibraryScreen(billing, ::openSession) }
                composable("progress") { ProgressScreen(journey) }
                composable("profile") { ProfileScreen(journey, billing, vm.billing, { upgrade() }, { nav.navigate("center") }, { nav.navigate("lab") }) }
                composable("session/{id}") { backStack ->
                    val session = sessions.first { it.id == backStack.arguments?.getString("id") }
                    // Recheck access when identity or entitlement changes while this destination is open.
                    if (session.premium && !billing.premium) {
                        LockedSessionScreen({ nav.popBackStack() }) { upgrade(session.id) }
                    } else SessionScreen(session, { nav.popBackStack() }) {
                        val shouldShow = vm.completeSession()
                        nav.navigate("complete") { popUpTo("session/{id}") { inclusive = true } }
                        if (shouldShow) upgrade()
                    }
                }
                composable("complete") { CompletionScreen(journey.completed) {
                    nav.navigate("home") { popUpTo("home") { inclusive = true } }
                } }
                composable("paywall") { CustomPaywallScreen(billing, { dismissPaywall() }, vm.billing::restore,
                    { plan -> vm.billing.purchase(activity, plan) }, { nav.navigate("hosted") },
                    { vm.billing.refresh(force = true) }, { policy ->
                        val url = if (policy == "privacy") BuildConfig.PRIVACY_URL else BuildConfig.TERMS_URL
                        if (url.isBlank()) scope.launch { snack.showSnackbar("This sample has no published $policy policy. Add your app’s policy URL before publishing.") }
                        else openWeb(activity, url)
                    }) }
                composable("hosted") { HostedPaywallScreen(billing, vm.billing) { nav.popBackStack() } }
                composable("center") { CustomerCenterScreen(billing, vm.billing) { vm.billing.refresh(force = true); nav.popBackStack() } }
                composable("lab") { SdkLabScreen(journey, billing, vm.billing, { nav.popBackStack() }) {
                    vm.resetJourney()
                    vm.billing.resetDemo()
                    nav.navigate("welcome") { popUpTo(nav.graph.id) { inclusive = true } }
                } }
            }
        }
    }
}
