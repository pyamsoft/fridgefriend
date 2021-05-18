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

package com.pyamsoft.fridge.db.room.migrate

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import timber.log.Timber

internal object Migrate1To2 : Migration(1, 2) {

  override fun migrate(database: SupportSQLiteDatabase) {
    performMigration(database) {
      Timber.d("Add ArchivedAt column to FridgeEntry")
      execSQL(
          "ALTER TABLE ${RoomFridgeEntry.TABLE_NAME} ADD COLUMN ${RoomFridgeEntry.COLUMN_ARCHIVED_AT} INTEGER")

      Timber.d("Drop nearby store")
      execSQL("DROP TABLE room_nearby_store_table")

      Timber.d("Drop nearby zone")
      execSQL("DROP TABLE room_nearby_zone_table")
    }
  }
}
