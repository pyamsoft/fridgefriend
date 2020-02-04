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
 *
 */

package com.pyamsoft.fridge.db.persist

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryInsertDao
import com.pyamsoft.fridge.db.category.FridgeCategoryQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject
import timber.log.Timber

internal class PersistentCategoriesImpl @Inject internal constructor(
    private val enforcer: Enforcer,
    private val queryDao: FridgeCategoryQueryDao,
    private val insertDao: FridgeCategoryInsertDao,
    private val preferences: PersistentCategoryPreferences
) : PersistentCategories {

    @CheckResult
    private suspend fun guaranteeCategoriesInserted(): List<FridgeCategory> {
        val inserted = preferences.isPersistentCategoriesInserted()
        if (!inserted) {
            Timber.d("Insert default categories")

            // Parallel collection iteration one day?
            DEFAULT_CATEGORIES.forEach { insertDao.insert(it) }

            preferences.setPersistentCategoriesInserted()
        }
        return queryDao.query(false).filter { it.isDefault() }
    }

    override suspend fun getPersistentCategories(): List<FridgeCategory> {
        enforcer.assertNotOnMainThread()
        return guaranteeCategoriesInserted()
    }

    companion object {

        private val DEFAULT_CATEGORIES = listOf(
            FridgeCategory.createDefault("Fruits"),
            FridgeCategory.createDefault("Vegetables"),
            FridgeCategory.createDefault("Meat"),
            FridgeCategory.createDefault("Fish"),
            FridgeCategory.createDefault("Dairy"),
            FridgeCategory.createDefault("Spices"),
            FridgeCategory.createDefault("Bread"),
            FridgeCategory.createDefault("Sweets"),
            FridgeCategory.createDefault("Drinks"),
            FridgeCategory.createDefault("Alcohol")
        )
    }
}
