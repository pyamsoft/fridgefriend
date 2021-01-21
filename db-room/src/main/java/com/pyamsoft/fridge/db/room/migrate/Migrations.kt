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
import timber.log.Timber

internal inline fun Migration.performMigration(
    database: SupportSQLiteDatabase,
    block: SupportSQLiteDatabase.() -> Unit
) {
    Timber.d("Migrating from ${this.startVersion} -> ${this.endVersion} starting")
    block(database)
    Timber.d("Migration from ${this.startVersion} -> ${this.endVersion} complete")
}

