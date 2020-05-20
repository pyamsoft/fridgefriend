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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.guarantee.EntryGuarantee
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemPreferences
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.db.persist.PersistentCategories
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.PreferenceListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal class DetailInteractor @Inject internal constructor(
    private val enforcer: Enforcer,
    private val entryGuarantee: EntryGuarantee,
    private val itemQueryDao: FridgeItemQueryDao,
    private val itemInsertDao: FridgeItemInsertDao,
    private val itemUpdateDao: FridgeItemUpdateDao,
    private val itemDeleteDao: FridgeItemDeleteDao,
    private val itemRealtime: FridgeItemRealtime,
    private val entryQueryDao: FridgeEntryQueryDao,
    private val persistentCategories: PersistentCategories,
    private val categoryQueryDao: FridgeCategoryQueryDao,
    private val preferences: FridgeItemPreferences
) {

    @CheckResult
    suspend fun isZeroCountConsideredConsumed(): Boolean =
        withContext(context = Dispatchers.Default) {
            return@withContext preferences.isZeroCountConsideredConsumed()
        }

    @CheckResult
    suspend fun getExpiringSoonRange(): Int = withContext(context = Dispatchers.Default) {
        return@withContext preferences.getExpiringSoonRange()
    }

    @CheckResult
    suspend fun isSameDayExpired(): Boolean = withContext(context = Dispatchers.Default) {
        return@withContext preferences.isSameDayExpired()
    }

    @CheckResult
    suspend fun listenForExpiringSoonRangeChanged(onChange: (newRange: Int) -> Unit): PreferenceListener =
        withContext(context = Dispatchers.Default) {
            return@withContext preferences.watchForExpiringSoonChange(onChange)
        }

    @CheckResult
    suspend fun listenForSameDayExpiredChanged(onChange: (newSameDay: Boolean) -> Unit): PreferenceListener =
        withContext(context = Dispatchers.Default) {
            return@withContext preferences.watchForSameDayExpiredChange(onChange)
        }

    @CheckResult
    suspend fun findSameNamedItems(name: String, presence: Presence): Collection<FridgeItem> =
        withContext(context = Dispatchers.Default) {
            enforcer.assertNotOnMainThread()
            return@withContext itemQueryDao.querySameNameDifferentPresence(false, name, presence)
        }

    @CheckResult
    suspend fun findSimilarNamedItems(item: FridgeItem): Collection<FridgeItem> =
        withContext(context = Dispatchers.Default) {
            enforcer.assertNotOnMainThread()
            return@withContext itemQueryDao.querySimilarNamedItems(false, item)
        }

    @CheckResult
    suspend fun loadAllCategories(): List<FridgeCategory> =
        withContext(context = Dispatchers.Default) {
            enforcer.assertNotOnMainThread()
            persistentCategories.guaranteePersistentCategoriesCreated()
            return@withContext categoryQueryDao.query(false)
        }

    @CheckResult
    suspend fun loadEntry(entryId: FridgeEntry.Id): FridgeEntry =
        withContext(context = Dispatchers.Default) {
            enforcer.assertNotOnMainThread()
            return@withContext entryQueryDao.query(false).first { it.id() == entryId }
        }

    @CheckResult
    suspend fun resolveItem(
        itemId: FridgeItem.Id,
        entryId: FridgeEntry.Id,
        presence: Presence,
        force: Boolean
    ): FridgeItem = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        return@withContext if (itemId.isEmpty()) {
            createNewItem(entryId, presence)
        } else {
            loadItem(itemId, entryId, force)
        }
    }

    /**
     * Create a new FridgeItem
     */
    @CheckResult
    private fun createNewItem(entryId: FridgeEntry.Id, presence: Presence): FridgeItem {
        enforcer.assertNotOnMainThread()
        return FridgeItem.create(entryId = entryId, presence = presence)
    }

    /**
     * If the itemId parameter is blank, this will crash the app.
     *
     * This should only be called on items that already exist in the db.
     */
    @CheckResult
    private suspend fun loadItem(
        itemId: FridgeItem.Id,
        entryId: FridgeEntry.Id,
        force: Boolean
    ): FridgeItem = getItems(entryId, force).first { it.id() == itemId }

    @CheckResult
    suspend fun getItems(
        entryId: FridgeEntry.Id,
        force: Boolean
    ): List<FridgeItem> = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        return@withContext itemQueryDao.query(force, entryId)
    }

    suspend fun listenForChanges(
        id: FridgeEntry.Id,
        onChange: suspend (event: FridgeItemChangeEvent) -> Unit
    ) =
        withContext(context = Dispatchers.Default) {
            enforcer.assertNotOnMainThread()
            return@withContext itemRealtime.listenForChanges(id, onChange)
        }

    suspend fun commit(item: FridgeItem) = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        if (FridgeItem.isValidName(item.name())) {
            val entry = entryGuarantee.existing(item.entryId(), FridgeEntry.DEFAULT_NAME)
            Timber.d("Guarantee entry exists: $entry")
            commitItem(item)
        } else {
            Timber.w("Do not commit invalid name FridgeItem: $item")
        }
    }

    @CheckResult
    private suspend fun getValidItem(item: FridgeItem): FridgeItem? {
        enforcer.assertNotOnMainThread()
        return getItems(item.entryId(), false)
            .singleOrNull { it.id() == item.id() }
    }

    private suspend fun commitItem(item: FridgeItem) {
        enforcer.assertNotOnMainThread()
        val valid = getValidItem(item)
        if (valid != null) {
            Timber.d("Update existing item [${item.id()}]: $item")
            itemUpdateDao.update(item)
        } else {
            Timber.d("Create new item [${item.id()}]: $item")
            itemInsertDao.insert(item)
        }
    }

    suspend fun delete(item: FridgeItem) = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        assert(item.isReal()) { "Cannot delete item that is not real: $item" }
        Timber.d("Deleting item [${item.id()}]: $item")
        itemDeleteDao.delete(item)
    }
}
