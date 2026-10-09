package com.aistudio.knockoutalarm.subscription

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.knockoutalarm.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service encapsulating subscription tier logic.
 * Supports Free and Pro tiers, with mock dev switches to test both modes.
 */
class SubscriptionService(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("knockout_subscription_prefs", Context.MODE_PRIVATE)

    private val _tierState = MutableStateFlow(
        if (prefs.getBoolean(PREF_IS_PRO, true)) SubscriptionTier.PRO else SubscriptionTier.FREE
    )
    val tierState: StateFlow<SubscriptionTier> = _tierState.asStateFlow()

    fun isPro(): Boolean = _tierState.value == SubscriptionTier.PRO

    fun setTier(tier: SubscriptionTier) {
        _tierState.value = tier
        prefs.edit().putBoolean(PREF_IS_PRO, tier == SubscriptionTier.PRO).apply()
    }

    fun canUsePunchTarget(target: Int): Boolean {
        if (isPro()) return true
        return target <= 30 // Free tier supports 10 or 30
    }

    fun canAddAlarm(currentAlarmCount: Int): Boolean {
        if (isPro()) return true
        return currentAlarmCount < 1 // Free tier supports 1 active alarm
    }

    companion object {
        private const val PREF_IS_PRO = "pref_is_pro_tier"
    }
}
