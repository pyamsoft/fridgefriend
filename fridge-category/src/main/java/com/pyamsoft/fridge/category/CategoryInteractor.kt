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

package com.pyamsoft.fridge.category

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryQueryDao
import com.pyamsoft.fridge.db.persist.PersistentCategories
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject

class CategoryInteractor @Inject internal constructor(
    private val enforcer: Enforcer,
    private val persistentCategories: PersistentCategories,
    private val categoryQueryDao: FridgeCategoryQueryDao
) {

    @CheckResult
    private suspend fun loadAllCategories(): List<FridgeCategory> {
        enforcer.assertNotOnMainThread()
        persistentCategories.guaranteePersistentCategoriesCreated()
        return categoryQueryDao.query(false)
    }

    // Large categories gets all list items
    @CheckResult
    suspend fun loadLargeCategories(): List<FridgeCategory> {
        // Empty map just so that we duplicate the list to avoid another function
        // tainting the backing data
        return loadAllCategories().map { it }
    }

    // Small categories gets all list items, except the first one
    // which is large only
    // AND
    // we add a fake placeholder item to the end of the list
    @CheckResult
    suspend fun loadSmallCategories(): List<FridgeCategory> {
        val allCategories = loadAllCategories()
        val withoutFirstIndex = allCategories.filterIndexed { index, _ -> index > 0 }
        return withoutFirstIndex + listOf(FridgeCategory.empty())
    }
}
