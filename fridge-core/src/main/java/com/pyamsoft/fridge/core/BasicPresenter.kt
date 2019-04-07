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

abstract class BasicPresenter<M : Any> protected constructor(
  private val initialModel: M,
  private val onUpdate: (model: M, oldModel: M?) -> Unit
) {

  private var model: ViewState<M>? = null

  protected fun updateModel(func: M.() -> Unit) {
    val oldModel = model
    val newModel = nonNullModel(oldModel).state.apply(func)
    onUpdate(newModel, oldModel?.state)
    model = ViewState(newModel)
  }

  @CheckResult
  private fun nonNullModel(state: ViewState<M>?): ViewState<M> {
    if (state == null) {
      return ViewState(initialModel)
    } else {
      return state.copy()
    }
  }

  private data class ViewState<M : Any>(val state: M)
}