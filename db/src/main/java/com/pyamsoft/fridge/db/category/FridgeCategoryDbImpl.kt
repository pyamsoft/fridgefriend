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

package com.pyamsoft.fridge.db.category

import com.pyamsoft.cachify.Cached
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.DbApi
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FridgeCategoryDbImpl @Inject internal constructor(
    @param:DbApi private val realQueryDao: Cached<List<FridgeCategory>>,
    @param:DbApi private val realInsertDao: FridgeCategoryInsertDao,
    @param:DbApi private val realDeleteDao: FridgeCategoryDeleteDao,
) : BaseDbImpl<
        FridgeCategoryChangeEvent,
        FridgeCategoryRealtime,
        FridgeCategoryQueryDao,
        FridgeCategoryInsertDao,
        FridgeCategoryDeleteDao>(), FridgeCategoryDb {

    override suspend fun listenForChanges(onChange: suspend (event: FridgeCategoryChangeEvent) -> Unit) {
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            onEvent(onChange)
        }
    }

    override suspend fun query(force: Boolean): List<FridgeCategory> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            if (force) {
                invalidate()
            }

            return@withContext realQueryDao.call()
        }

    override suspend fun insert(o: FridgeCategory): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realInsertDao.insert(o).also { inserted ->
                if (inserted) {
                    publish(FridgeCategoryChangeEvent.Insert(o))
                } else {
                    publish(FridgeCategoryChangeEvent.Update(o))
                }
            }
        }

    override suspend fun delete(o: FridgeCategory, offerUndo: Boolean): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realDeleteDao.delete(o, offerUndo).also { deleted ->
                if (deleted) {
                    publish(FridgeCategoryChangeEvent.Delete(o, offerUndo))
                }
            }
        }

    override fun realtime(): FridgeCategoryRealtime {
        return this
    }

    override fun queryDao(): FridgeCategoryQueryDao {
        return this
    }

    override fun insertDao(): FridgeCategoryInsertDao {
        return this
    }

    override fun deleteDao(): FridgeCategoryDeleteDao {
        return this
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        realQueryDao.clear()
    }

}
