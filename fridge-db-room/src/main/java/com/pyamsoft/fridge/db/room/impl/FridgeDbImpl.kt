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

import com.pyamsoft.fridge.db.FridgeDbDeleteDao
import com.pyamsoft.fridge.db.FridgeDbInsertDao
import com.pyamsoft.fridge.db.FridgeDbQueryDao
import com.pyamsoft.fridge.db.FridgeDbRealtime
import com.pyamsoft.fridge.db.FridgeDbUpdateDao

internal class FridgeDbImpl internal constructor(
  private val roomImpl: FridgeDb
) : FridgeDb {

  override fun realtime(): FridgeDbRealtime {
    return roomImpl.realtime()
  }

  override fun query(): FridgeDbQueryDao {
    return roomImpl.query()
  }

  override fun insert(): FridgeDbInsertDao {
    return roomImpl.insert()
  }

  override fun update(): FridgeDbUpdateDao {
    return roomImpl.update()
  }

  override fun delete(): FridgeDbDeleteDao {
    return roomImpl.delete()
  }

}