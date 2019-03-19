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

package com.pyamsoft.fridge.db.room.impl

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.FridgeDbDeleteDao
import com.pyamsoft.fridge.db.FridgeDbInsertDao
import com.pyamsoft.fridge.db.FridgeDbQueryDao
import com.pyamsoft.fridge.db.FridgeDbRealtime
import com.pyamsoft.fridge.db.FridgeDbUpdateDao

internal interface FridgeDb {

  @CheckResult
  fun realtime(): FridgeDbRealtime

  @CheckResult
  fun query(): FridgeDbQueryDao

  @CheckResult
  fun insert(): FridgeDbInsertDao

  @CheckResult
  fun update(): FridgeDbUpdateDao

  @CheckResult
  fun delete(): FridgeDbDeleteDao

}