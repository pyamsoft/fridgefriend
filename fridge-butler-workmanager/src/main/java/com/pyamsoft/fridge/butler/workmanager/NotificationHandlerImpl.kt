/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.fridge.butler.workmanager

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationHandler.Page
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NotificationHandlerImpl @Inject internal constructor(
    private val context: Context,
    private val activityClass: Class<out Activity>
) : NotificationHandler {

    override fun contentIntent(page: Page): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            putExtra(NotificationHandler.CONTENT_KEY_PAGE, page.name)
        }

        return PendingIntent.getActivity(
            context,
            CONTENT_INTENT_RC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {

        private const val CONTENT_INTENT_RC = 1589
    }
}
