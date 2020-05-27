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

package com.pyamsoft.fridge.db.category

import androidx.annotation.CheckResult
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class JsonMappableFridgeCategory internal constructor(
    internal val id: FridgeCategory.Id,
    internal val name: String,
    internal val createdTime: Date,
    internal val thumbnail: FridgeCategory.Thumbnail?,
    internal val isDefault: Boolean
) : FridgeCategory {

    override fun id(): FridgeCategory.Id {
        return id
    }

    override fun name(): String {
        return name
    }

    override fun createdTime(): Date {
        return createdTime
    }

    override fun thumbnail(): FridgeCategory.Thumbnail? {
        return thumbnail
    }

    override fun isDefault(): Boolean {
        return isDefault
    }

    override fun isEmpty(): Boolean {
        return id().isEmpty()
    }

    override fun name(name: String): FridgeCategory {
        return this.copy(name = name.trim())
    }

    override fun invalidateThumbnail(): FridgeCategory {
        return this.copy(thumbnail = null)
    }

    override fun thumbnail(thumbnail: FridgeCategory.Thumbnail): FridgeCategory {
        return this.copy(thumbnail = thumbnail)
    }

    companion object {

        @JvmStatic
        @CheckResult
        fun from(category: FridgeCategory): JsonMappableFridgeCategory {
            return if (category is JsonMappableFridgeCategory) category else {
                JsonMappableFridgeCategory(
                    category.id(),
                    category.name(),
                    category.createdTime(),
                    category.thumbnail(),
                    category.isDefault()
                )
            }
        }
    }
}
