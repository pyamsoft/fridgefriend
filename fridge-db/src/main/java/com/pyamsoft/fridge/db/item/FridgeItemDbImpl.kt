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

package com.pyamsoft.fridge.db.item

import androidx.annotation.CheckResult
import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.math.min

internal class FridgeItemDbImpl internal constructor(
    private val db: FridgeItemDb,
    private val cache: Cached1<Sequence<FridgeItem>, Boolean>
) : FridgeItemDb {

    private val mutex = Mutex()

    private suspend fun publishRealtime(event: FridgeItemChangeEvent) {
        cache.clear()
        publish(event)
    }

    override suspend fun publish(event: FridgeItemChangeEvent) {
        db.publish(event)
    }

    override fun realtime(): FridgeItemRealtime {
        return db.realtime()
    }

    override fun queryDao(): FridgeItemQueryDao {
        return object : FridgeItemQueryDao {

            @CheckResult
            private suspend fun queryAsSequence(force: Boolean): Sequence<FridgeItem> {
                if (force) {
                    cache.clear()
                }

                return cache.call(force)
            }

            override suspend fun query(force: Boolean): List<FridgeItem> {
                mutex.withLock {
                    return queryAsSequence(force).toList()
                }
            }

            override suspend fun query(
                force: Boolean,
                entryId: String
            ): List<FridgeItem> {
                mutex.withLock {
                    return queryAsSequence(force)
                        .filter { it.entryId() == entryId }
                        .toList()
                }
            }

            override suspend fun querySameNameDifferentPresence(
                force: Boolean,
                name: String,
                presence: Presence
            ): List<FridgeItem> {
                mutex.withLock {
                    return queryAsSequence(force)
                        .filter { it.isReal() }
                        .filter { it.presence() == presence }
                        .filter { item ->
                            val cleanName = name.toLowerCase().trim()
                            val itemName = item.name().toLowerCase().trim()
                            return@filter itemName == cleanName
                        }
                        .toList()
                }
            }

            override suspend fun querySimilarNamedItems(force: Boolean, item: FridgeItem): List<FridgeItem> {
                mutex.withLock {
                    return queryAsSequence(force)
                        .filter { it.isReal() }
                        .filterNot { it.id() == item.id() }
                        .filter { fridgeItem ->
                            val name = item.name().toLowerCase().trim()
                            val itemName = fridgeItem.name().toLowerCase().trim()
                            return@filter itemName == name ||
                                itemName.startsWith(name) ||
                                itemName.endsWith(name) ||
                                itemName.contains(name) ||
                                itemName.withDistanceRatio(name, 0.65F)
                        }
                        .toList()
                }
            }

            @CheckResult
            private fun String.withDistanceRatio(str: String, accepted: Float): Boolean {
                // Initialize a zero-matrix
                val s1Len = this.length
                val s2Len = str.length
                val rows = s1Len + 1
                val columns = s2Len + 1
                val matrix = Array(rows) { IntArray(columns) { 0 } }

                // Populate matrix with indices of each character in strings
                for (i in 1 until rows) {
                    matrix[i][0] = i
                }

                for (j in 1 until columns) {
                    matrix[0][j] = j
                }

                // Calculate the cost of deletes, inserts, and subs
                for (col in 1 until columns) {
                    for (row in 1 until rows) {
                        // If the character is the same in a given position, cost is 0, else cost is 2
                        val cost = if (this[row - 1] == str[col - 1]) 0 else 2

                        // The cost of a deletion, insertion, and substitution
                        val deleteCost = matrix[row - 1][col] + 1
                        val insertCost = matrix[row][col - 1] + 1
                        val substitutionCost = matrix[row - 1][col - 1] + cost

                        // Populate the matrix
                        matrix[row][col] = min(deleteCost, min(insertCost, substitutionCost))
                    }
                }

                // Calculate distance ratio
                val totalLength = (s1Len + s2Len)
                val ratio = (totalLength - matrix[s1Len][s2Len]).toFloat() / totalLength
                val closeEnough = ratio >= accepted
                if (closeEnough) {
                    Timber.d("Within distance ratio: '${this}' <=> '${str}' [$ratio]")
                }
                return closeEnough
            }
        }
    }

    override fun insertDao(): FridgeItemInsertDao {
        return object : FridgeItemInsertDao {

            override suspend fun insert(o: FridgeItem) {
                mutex.withLock {
                    db.insertDao()
                        .insert(o)
                    publishRealtime(Insert(o.makeReal()))
                }
            }
        }
    }

    override fun updateDao(): FridgeItemUpdateDao {
        return object : FridgeItemUpdateDao {

            override suspend fun update(o: FridgeItem) {
                mutex.withLock {
                    db.updateDao()
                        .update(o)
                    publishRealtime(Update(o.makeReal()))
                }
            }
        }
    }

    override fun deleteDao(): FridgeItemDeleteDao {
        return object : FridgeItemDeleteDao {

            override suspend fun delete(o: FridgeItem) {
                mutex.withLock {
                    db.deleteDao()
                        .delete(o)
                    publishRealtime(Delete(o.makeReal()))
                }
            }
        }
    }
}
