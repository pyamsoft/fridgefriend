package com.pyamsoft.fridge.butler.runner

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.params.EmptyParameters
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class NightlyRunner @Inject internal constructor(
    handler: NotificationHandler,
    butler: Butler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
    enforcer: Enforcer,
    private val fridgeItemQueryDao: FridgeItemQueryDao
) : BaseRunner<EmptyParameters>(
    handler,
    butler,
    notificationPreferences,
    butlerPreferences,
    enforcer
) {

    private suspend fun notifyNightly(
        items: List<FridgeItem>,
        now: Calendar,
        preferences: ButlerPreferences
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeNightly()
            if (now.isAllowedToNotify(false, lastTime) && isAtleastTime(now)) {
                Timber.d("Notify user about items nightly fridge cleanup")
                notification { handler ->
                    if (handler.notifyNightly()) {
                        preferences.markNotificationNightly(now)
                    }
                }
            }
        }
    }

    override suspend fun reschedule(butler: Butler, params: EmptyParameters) {
        butler.scheduleRemindNightly(params)
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: EmptyParameters
    ) = coroutineScope {
        val now = today()
        val items = fridgeItemQueryDao.query(false)
        notifyNightly(items.filter { it.presence() == HAVE }, now, preferences)
    }

    @CheckResult
    private fun isAtleastTime(now: Calendar): Boolean {
        val hour = now.get(Calendar.HOUR_OF_DAY)
        return hour >= EVENING_HOUR
    }

    companion object {
        // 8PM
        private const val EVENING_HOUR = 8 + 12
    }
}
