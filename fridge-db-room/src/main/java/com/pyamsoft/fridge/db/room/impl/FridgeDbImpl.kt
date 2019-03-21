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

import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao

internal class FridgeDbImpl internal constructor(
  private val roomImpl: FridgeDb
) : FridgeDb {

  override fun realtime(): FridgeItemRealtime {
    return roomImpl.realtime()
  }

  override fun query(): FridgeItemQueryDao {
    return roomImpl.query()
  }

  override fun insert(): FridgeItemInsertDao {
    return roomImpl.insert()
  }

  override fun update(): FridgeItemUpdateDao {
    return roomImpl.update()
  }

  override fun delete(): FridgeItemDeleteDao {
    return roomImpl.delete()
  }

}