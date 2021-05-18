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

package com.pyamsoft.fridge.db.room

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.room.dao.category.RoomFridgeCategoryDeleteDao
import com.pyamsoft.fridge.db.room.dao.category.RoomFridgeCategoryInsertDao
import com.pyamsoft.fridge.db.room.dao.category.RoomFridgeCategoryQueryDao
import com.pyamsoft.fridge.db.room.dao.entry.RoomFridgeEntryDeleteDao
import com.pyamsoft.fridge.db.room.dao.entry.RoomFridgeEntryInsertDao
import com.pyamsoft.fridge.db.room.dao.entry.RoomFridgeEntryQueryDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemDeleteDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemInsertDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemQueryDao

internal interface RoomFridgeDb {

  @CheckResult fun roomItemQueryDao(): RoomFridgeItemQueryDao

  @CheckResult fun roomItemInsertDao(): RoomFridgeItemInsertDao

  @CheckResult fun roomItemDeleteDao(): RoomFridgeItemDeleteDao

  @CheckResult fun roomEntryQueryDao(): RoomFridgeEntryQueryDao

  @CheckResult fun roomEntryInsertDao(): RoomFridgeEntryInsertDao

  @CheckResult fun roomEntryDeleteDao(): RoomFridgeEntryDeleteDao

  @CheckResult fun roomCategoryQueryDao(): RoomFridgeCategoryQueryDao

  @CheckResult fun roomCategoryInsertDao(): RoomFridgeCategoryInsertDao

  @CheckResult fun roomCategoryDeleteDao(): RoomFridgeCategoryDeleteDao
}
