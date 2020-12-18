/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.db.category

import com.pyamsoft.cachify.Cached
import com.pyamsoft.fridge.db.fridge.BaseDbImpl
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class FridgeCategoryDbImpl internal constructor(
    private val cache: Cached<List<FridgeCategory>>,
    insertDao: FridgeCategoryInsertDao,
    deleteDao: FridgeCategoryDeleteDao
) : BaseDbImpl<FridgeCategory, FridgeCategoryChangeEvent>(), FridgeCategoryDb {

    private val realtime = object : FridgeCategoryRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: FridgeCategoryChangeEvent) -> Unit) {
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                onEvent(onChange)
            }
        }
    }

    private val queryDao = object : FridgeCategoryQueryDao {

        override suspend fun query(force: Boolean): List<FridgeCategory> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                if (force) {
                    invalidate()
                }

                return@withContext cache.call()
            }
    }

    private val insertDao = object : FridgeCategoryInsertDao {

        override suspend fun insert(o: FridgeCategory): Boolean =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext insertDao.insert(o).also { inserted ->
                    if (inserted) {
                        publish(FridgeCategoryChangeEvent.Insert(o))
                    } else {
                        publish(FridgeCategoryChangeEvent.Update(o))
                    }
                }
            }
    }

    private val deleteDao = object : FridgeCategoryDeleteDao {

        override suspend fun delete(o: FridgeCategory, offerUndo: Boolean): Boolean =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext deleteDao.delete(o, offerUndo).also { deleted ->
                    if (deleted) {
                        publish(FridgeCategoryChangeEvent.Delete(o, offerUndo))
                    }
                }
            }
    }

    override fun realtime(): FridgeCategoryRealtime {
        return realtime
    }

    override fun queryDao(): FridgeCategoryQueryDao {
        return queryDao
    }

    override fun insertDao(): FridgeCategoryInsertDao {
        return insertDao
    }

    override fun deleteDao(): FridgeCategoryDeleteDao {
        return deleteDao
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        cache.clear()
    }
}
