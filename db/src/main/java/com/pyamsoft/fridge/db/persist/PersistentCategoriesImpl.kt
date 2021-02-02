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

package com.pyamsoft.fridge.db.persist

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import com.pyamsoft.fridge.db.R
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryInsertDao
import com.pyamsoft.fridge.db.category.toThumbnail
import com.pyamsoft.fridge.preference.PersistentPreferences
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume

internal class PersistentCategoriesImpl @Inject internal constructor(
    private val imageLoader: ImageLoader,
    private val insertDao: FridgeCategoryInsertDao,
    private val preferences: PersistentPreferences,
) : PersistentCategories {

    private suspend fun guaranteeCategoriesInserted() {
        val inserted = preferences.isPersistentCategoriesInserted()
        if (!inserted) {
            Timber.d("Insert default categories")

            val defaultCategories = listOf(
                FridgeCategory.createDefault(
                    "Fruits",
                    loadImage(R.drawable.category_thumbnail_fruits)
                ),
                FridgeCategory.createDefault(
                    "Vegetables",
                    loadImage(R.drawable.category_thumbnail_vegetables)
                ),
                FridgeCategory.createDefault("Meat", loadImage(R.drawable.category_thumbnail_meat)),
                FridgeCategory.createDefault("Fish", loadImage(R.drawable.category_thumbnail_fish)),
                FridgeCategory.createDefault(
                    "Dairy",
                    loadImage(R.drawable.category_thumbnail_dairy)
                ),
                FridgeCategory.createDefault(
                    "Spices",
                    loadImage(R.drawable.category_thumbnail_spices)
                ),
                FridgeCategory.createDefault(
                    "Bread",
                    loadImage(R.drawable.category_thumbnail_bread)
                ),
                FridgeCategory.createDefault(
                    "Sweets",
                    loadImage(R.drawable.category_thumbnail_sweets)
                ),
                FridgeCategory.createDefault(
                    "Drinks",
                    loadImage(R.drawable.category_thumbnail_drinks)
                ),
                FridgeCategory.createDefault(
                    "Alcohol",
                    loadImage(R.drawable.category_thumbnail_alcohol)
                )
            )

            // Parallel collection iteration one day?
            defaultCategories.forEach { category ->
                if (insertDao.insert(category)) {
                    Timber.d("Inserted new default category: $category")
                } else {
                    Timber.d("Updated existing default category: $category")
                }
            }

            preferences.setPersistentCategoriesInserted()
        }
    }

    override suspend fun guaranteePersistentCategoriesCreated() =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            return@withContext guaranteeCategoriesInserted()
        }

    @CheckResult
    private suspend fun coroutineGlide(@DrawableRes res: Int): Drawable? {
        Enforcer.assertOffMainThread()
        return suspendCancellableCoroutine { continuation ->
            Enforcer.assertOffMainThread()
            imageLoader.load(res)
                .onError {
                    Timber.e("Error occurred while loading image thumbnail for default category")
                    continuation.resume(null)
                }
                .into(object : ImageTarget<Drawable> {

                    override fun clear() {
                    }

                    override fun setImage(image: Drawable) {
                        continuation.resume(image)
                    }
                })
        }
    }

    @CheckResult
    private suspend fun loadImage(@DrawableRes res: Int): FridgeCategory.Thumbnail? {
        Enforcer.assertOffMainThread()
        val image: Drawable? = coroutineGlide(res)
        return if (image is BitmapDrawable) {
            Timber.d("Compress bitmap drawable to PNG for blob storage")
            val data = ByteArrayOutputStream().run {
                image.bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                toByteArray()
            }
            data.toThumbnail()
        } else {
            Timber.w("Image loaded a drawable but could not convert it to bytes")
            null
        }
    }
}
