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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.IdGenerator
import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.db.EmptyModel
import java.util.Date

interface FridgeCategory : EmptyModel<FridgeCategory> {

    @CheckResult
    fun id(): Id

    @CheckResult
    fun isDefault(): Boolean

    @CheckResult
    fun thumbnail(): Thumbnail?

    @CheckResult
    fun invalidateThumbnail(): FridgeCategory

    @CheckResult
    fun thumbnail(thumbnail: Thumbnail): FridgeCategory

    data class Id(val id: String) {

        @CheckResult
        fun isEmpty(): Boolean {
            return id.isBlank()
        }

        companion object {

            @JvmField
            val EMPTY = Id("")
        }
    }

    data class Thumbnail internal constructor(val data: ByteArray) {

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }

            if (javaClass != other?.javaClass) {
                return false
            }

            if (other is Thumbnail) {
                return data.contentEquals(other.data)
            }

            return false
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        // Don't output the data in the array or else logs will be super slow
        override fun toString(): String {
            return "Thumbnail(data=[${data.size}])"
        }

        companion object {

            @JvmStatic
            @CheckResult
            fun create(data: ByteArray): Thumbnail {
                return Thumbnail(data)
            }
        }
    }

    companion object {

        private val EMPTY_CATEGORY =
            JsonMappableFridgeCategory(Id.EMPTY, "", currentDate(), null, isDefault = true)

        @CheckResult
        fun empty(): FridgeCategory {
            return EMPTY_CATEGORY
        }

        @CheckResult
        fun create(
            id: Id,
            name: String,
        ): FridgeCategory {
            return JsonMappableFridgeCategory(id, name, currentDate(), null, isDefault = false)
        }

        @CheckResult
        fun createDefault(name: String, thumbnail: Thumbnail?): FridgeCategory {
            return JsonMappableFridgeCategory(
                Id(IdGenerator.generate()),
                name,
                currentDate(),
                thumbnail,
                isDefault = true
            )
        }

        @CheckResult
        @JvmOverloads
        fun create(
            category: FridgeCategory,
            name: String = category.name(),
            createdTime: Date = category.createdTime(),
            thumbnail: Thumbnail? = category.thumbnail(),
            isDefault: Boolean = category.isDefault(),
        ): FridgeCategory {
            return JsonMappableFridgeCategory(
                category.id(),
                name,
                createdTime,
                thumbnail,
                isDefault
            )
        }
    }
}

@CheckResult
fun FridgeCategory.thumbnail(data: ByteArray): FridgeCategory {
    return this.thumbnail(data.toThumbnail())
}

@CheckResult
fun ByteArray.toThumbnail(): FridgeCategory.Thumbnail {
    return FridgeCategory.Thumbnail.create(this)
}
