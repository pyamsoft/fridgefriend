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

package com.pyamsoft.fridge.core

import androidx.annotation.CheckResult
import com.mikepenz.fastadapter.IModelItem
import com.mikepenz.fastadapter.commons.utils.DiffCallback

abstract class DataClassDiffCallback<T : IModelItem<*, *, *>> private constructor() :
  DiffCallback<T> {

  final override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
    return oldItem.identifier == newItem.identifier
  }

  final override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
    return oldItem.model == newItem.model
  }

  final override fun getChangePayload(
    oldItem: T,
    oldItemPosition: Int,
    newItem: T,
    newItemPosition: Int
  ): Any? {
    return null
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun <T : IModelItem<*, *, *>> create(): DataClassDiffCallback<T> {
      return object : DataClassDiffCallback<T>() {}
    }
  }

}
