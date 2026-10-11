package com.example.mindfulplus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.mindfulplus.billing.AccessPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Session(val id: String, val title: String, val subtitle: String, val minutes: Int, val premium: Boolean)
val sessions = listOf(
    Session("morning", "Morning reset", "A little space before the day begins.", 5, false),
    Session("focus", "Deep focus", "Find your way back to one thing at a time.", 12, true),
    Session("evening", "Evening exhale", "Let the day settle. Make room for rest.", 8, true),
    Session("quiet", "A moment of quiet", "An easy pause, wherever you are.", 3, false),
)
data class JourneyState(val onboarded: Boolean, val goal: String, val completed: Int)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val billing = (application as MindfulApplication).billing
    private val preferences = application.getSharedPreferences("journey", Application.MODE_PRIVATE)
    private val mutable = MutableStateFlow(JourneyState(
        preferences.getBoolean("onboarded", false), preferences.getString("goal", "Feel calmer") ?: "Feel calmer",
        preferences.getInt("completed", 0),
    ))
    val journey = mutable.asStateFlow()
    fun chooseGoal(goal: String) {
        preferences.edit().putString("goal", goal).apply()
        mutable.update { it.copy(goal = goal) }
    }
    fun finishOnboarding() {
        preferences.edit().putBoolean("onboarded", true).apply()
        mutable.update { it.copy(onboarded = true) }
    }
    fun completeSession(): Boolean {
        mutable.update { it.copy(completed = it.completed + 1) }
        preferences.edit().putInt("completed", mutable.value.completed).apply()
        return AccessPolicy.shouldOfferUpgrade(mutable.value.completed, billing.state.value.premium)
    }
    fun resetJourney() {
        preferences.edit().clear().apply()
        mutable.value = JourneyState(false, "Feel calmer", 0)
    }
}
