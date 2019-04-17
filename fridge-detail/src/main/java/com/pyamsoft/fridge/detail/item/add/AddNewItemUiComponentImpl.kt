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

package com.pyamsoft.fridge.detail.item.add

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponent.Callback
import com.pyamsoft.fridge.detail.item.add.AddNewItemViewModel.AddNewState
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.arch.renderOnChange
import javax.inject.Inject

internal class AddNewItemUiComponentImpl @Inject internal constructor(
  private val itemView: AddNewItemView,
  private val viewModel: AddNewItemViewModel
) : BaseUiComponent<Callback>(),
  AddNewItemUiComponent {

  override fun id(): Int {
    return itemView.id()
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      itemView.teardown()
      viewModel.unbind()
    }

    itemView.inflate(savedInstanceState)
    viewModel.bind { state, oldState ->
      renderAddNew(state, oldState)
    }
  }

  override fun onSaveState(outState: Bundle) {
    itemView.saveState(outState)
  }

  private fun renderAddNew(
    state: AddNewState,
    oldState: AddNewState?
  ) {
    state.renderOnChange(oldState, value = { it.isAdding }) { adding ->
      if (adding) {
        callback.onAddNewItem()
      }
    }
  }

}
