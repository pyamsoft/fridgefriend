/*
 * Copyright 2021 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.butler.notification

enum class NotificationChannelInfo(
    val id: String,
    val title: String,
    val description: String
) {
    EXPIRING(
        id = "fridge_expiring_reminders_channel_v1",
        title = "Expiring Reminders",
        description = "Reminders for items that are going to expire soon"
    ),
    EXPIRED(
        id = "fridge_expiration_reminders_channel_v1",
        title = "Expired Reminders",
        description = "Reminders for items that have expired"
    ),
    NEEDED(
        id = "fridge_needed_reminders_channel_v1",
        title = "Shopping Reminders",
        description = "Reminders for items that you still need."
    ),
    NIGHTLY(
        id = "fridge_nightly_reminders_channel_v1",
        title = "Nightly Reminders",
        description = "Regular reminders each night to clean out your fridge"
    ),
}
