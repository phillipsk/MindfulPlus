package com.example.mindfulplus.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mindfulplus.*
import com.example.mindfulplus.billing.*

fun openWeb(context: Context, url: String) {
    val uri = Uri.parse(url)
    if (uri.scheme in listOf("https", "http")) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: android.content.ActivityNotFoundException) { }
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}

@Composable
fun PageHeader(label: String, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        } else Icon(Icons.Outlined.Spa, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun Eyebrow(text: String) = Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = Muted)

@Composable
fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) { Text(label) }
}

@Composable
fun InfoCard(title: String, body: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Soft) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

@Composable
fun ModeBadge(isDemo: Boolean) {
    Surface(color = Lime, shape = CircleShape) {
        Text(if (isDemo) "DEMO · NO CHARGES" else "LIVE BILLING", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun WelcomeScreen(demo: Boolean, next: () -> Unit) = Page {
    PageHeader("mindful+")
    Spacer(Modifier.height(12.dp))
    BotanicalArt(Modifier.height(220.dp))
    ModeBadge(demo)
    Text("A softer start.\nA clearer mind.", style = MaterialTheme.typography.displaySmall)
    Text("Make a little room for yourself. Build a daily ritual with short, thoughtful moments of calm.", style = MaterialTheme.typography.bodyLarge, color = Muted)
    PrimaryButton("Find my rhythm", onClick = next)
    Text("A sample wellness app with guided text previews.", style = MaterialTheme.typography.bodySmall, color = Muted)
}

@Composable
fun GoalScreen(selected: String, choose: (String) -> Unit, back: () -> Unit, next: () -> Unit) = Page {
    PageHeader("Your intention", back)
    Eyebrow("01 / 02 · make it yours")
    Text("What brings\nyou here?", style = MaterialTheme.typography.displaySmall)
    Text("There’s no perfect answer. Pick what feels right today.", color = Muted)
    listOf("Feel calmer", "Find focus", "Rest more easily").forEach { goal ->
        Surface(Modifier.fillMaxWidth().clickable { choose(goal) }, shape = RoundedCornerShape(22.dp),
            color = if (goal == selected) Lime else Soft, border = if (goal == selected) BorderStroke(1.dp, Forest) else null) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(goal, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                RadioButton(selected = goal == selected, onClick = { choose(goal) })
            }
        }
    }
    PrimaryButton("Continue", onClick = next)
}

@Composable
fun PlanScreen(goal: String, back: () -> Unit, next: () -> Unit) = Page {
    PageHeader("A plan for you", back)
    Eyebrow("02 / 02 · start small")
    Text("Your daily\nbreathing space.", style = MaterialTheme.typography.displaySmall)
    BotanicalArt()
    InfoCard("Your intention: $goal", "Begin with Morning reset. Explore its three guided steps, then discover the rest of your library.")
    Text("Free sessions are always here. Plus opens the full collection whenever you’re ready.", color = Muted)
    PrimaryButton("Let’s begin", onClick = next)
}

@Composable
fun HomeScreen(journey: JourneyState, billing: BillingState, start: () -> Unit, explore: () -> Unit, upgrade: () -> Unit) = Page {
    PageHeader("mindful+")
    Eyebrow("A little space for today")
    Text("Come back\nto yourself.", style = MaterialTheme.typography.displaySmall)
    Text("Your intention · ${journey.goal}", color = Muted)
    Surface(Modifier.fillMaxWidth(), color = Forest, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BotanicalArt(dark = true)
            Text("YOUR DAILY PRACTICE", color = Lime, style = MaterialTheme.typography.labelSmall)
            Text("Morning reset", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("5 min · guided text preview · free", color = Color(0xFFC4D1C7))
            Button(onClick = start, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Forest), modifier = Modifier.fillMaxWidth()) { Text("Begin session") }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Keep exploring", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = explore) { Text("View library") }
    }
    if (!billing.premium) InfoCard("A little more room with Plus", "Focus rituals, evening wind-downs, and every guided session.", Modifier.clickable(onClick = upgrade))
    else InfoCard("Welcome to Plus", "Your full library is ready. Choose the moment you need.")
    ModeBadge(billing.isDemo)
}

@Composable
fun LibraryScreen(billing: BillingState, openSession: (String) -> Unit) = Page {
    PageHeader("Your library")
    Text("Find your\nnext moment.", style = MaterialTheme.typography.displaySmall)
    Text("Short rituals for a fuller day.", color = Muted)
    sessions.forEach { session ->
        Surface(Modifier.fillMaxWidth().clickable { openSession(session.id) }.testTag("session_${session.id}"), shape = RoundedCornerShape(24.dp), color = if (session.premium) Soft else Lime) {
            Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(session.title, style = MaterialTheme.typography.titleLarge)
                    Text(session.subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
                    Text("${session.minutes} min · ${if (session.premium) "Plus" else "Free"}", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                Icon(if (session.premium && !billing.premium) Icons.Outlined.Lock else Icons.Outlined.PlayCircle, contentDescription = if (session.premium && !billing.premium) "Plus session locked" else "Open session")
            }
        }
    }
}

@Composable
fun SessionScreen(session: Session, back: () -> Unit, complete: () -> Unit) {
    var step by rememberSaveable(session.id) { mutableIntStateOf(0) }
    val instructions = when (session.id) {
        "focus" -> listOf("Choose one thing" to "Set other tasks aside for a moment. Notice the task you want to give your attention to.", "Return gently" to "Let your breathing settle into its own rhythm. When your attention wanders, bring it back without judgment.", "Begin again" to "Choose one small next action. Carry this quieter focus into it.")
        "evening" -> listOf("Let the day settle" to "Find a comfortable place to sit. Allow your shoulders to soften.", "Make room for rest" to "Notice a steady, comfortable breath. Let the day’s unfinished thoughts pass without following them.", "Close with kindness" to "Name one thing you appreciated today. Give yourself permission to pause.")
        else -> listOf("Settle in" to "Find a comfortable seat. Rest your hands. Let your shoulders soften.", "Notice your breath" to "Breathe naturally. Notice each inhale and exhale without trying to change them.", "Take it with you" to "Notice how you feel. Choose one small intention for the next part of your day.")
    }
    Page {
        PageHeader(session.title, back)
        Eyebrow("Guided text preview · step ${step + 1} of 3")
        BotanicalArt()
        Text(instructions[step].first, style = MaterialTheme.typography.displaySmall)
        Text(instructions[step].second, style = MaterialTheme.typography.bodyLarge, color = Muted)
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth(), color = Forest, trackColor = Soft)
        Text("Take your time. Move on whenever you’re ready.", style = MaterialTheme.typography.bodySmall, color = Muted)
        PrimaryButton(if (step < 2) "Next step" else "Finish session") { if (step < 2) step++ else complete() }
    }
}

@Composable
fun LockedSessionScreen(back: () -> Unit, upgrade: () -> Unit) = Page {
    PageHeader("Plus session", back)
    BotanicalArt()
    Text("Make a little\nmore room.", style = MaterialTheme.typography.displaySmall)
    Text("This session is part of Plus. Your free sessions are still available in the library.", color = Muted)
    PrimaryButton("Explore Plus", onClick = upgrade)
}

@Composable
fun CompletionScreen(count: Int, done: () -> Unit) = Page {
    PageHeader("A moment, well spent")
    BotanicalArt()
    Text("Small pauses.\nReal possibility.", style = MaterialTheme.typography.displaySmall)
    Text("You’ve finished $count guided ${if (count == 1) "preview" else "previews"}. Let this be enough for today, or explore another.", color = Muted)
    PrimaryButton("Back to today", onClick = done)
}

@Composable
fun ProgressScreen(journey: JourneyState) = Page {
    PageHeader("Your progress")
    Text("Every pause\ncounts.", style = MaterialTheme.typography.displaySmall)
    BotanicalArt()
    Surface(color = Lime, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(26.dp)) {
            Text(journey.completed.toString(), style = MaterialTheme.typography.displaySmall)
            Text("guided previews completed", color = Muted)
        }
    }
    InfoCard("Your intention", journey.goal)
    Text("Progress is saved on this device. These previews are text exercises; the app does not track meditation time.", style = MaterialTheme.typography.bodySmall, color = Muted)
}

@Composable
fun ProfileScreen(journey: JourneyState, state: BillingState, billing: BillingRepository, upgrade: () -> Unit, center: () -> Unit, lab: () -> Unit) = Page {
    val context = LocalContext.current
    PageHeader("Your space")
    Text("A practice\nthat’s yours.", style = MaterialTheme.typography.displaySmall)
    ModeBadge(state.isDemo)
    InfoCard(if (state.premium) "MindfulPlus member" else "Free membership", state.subscriptionSummary)
    InfoCard("Your intention", journey.goal)
    PrimaryButton(if (state.premium) "Explore membership plans" else "Discover Plus", onClick = upgrade)
    OutlinedButton(onClick = center, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Customer Center") }
    OutlinedButton(onClick = billing::restore, modifier = Modifier.fillMaxWidth(), enabled = !state.busy && state.configured) { Text("Restore purchases") }
    TextButton(onClick = { billing.manageSubscriptions(context) }, enabled = !state.busy && state.configured) { Text("Manage store subscription") }
    TextButton(onClick = lab) { Icon(Icons.Outlined.Science, null); Spacer(Modifier.width(8.dp)); Text("Open SDK lab") }
}
