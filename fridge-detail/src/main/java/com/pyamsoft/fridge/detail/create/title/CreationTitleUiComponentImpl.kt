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

package com.pyamsoft.fridge.detail.create.title

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.create.title.CreationTitlePresenter.NameState
import com.pyamsoft.fridge.detail.create.title.CreationTitleUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import javax.inject.Inject

internal class CreationTitleUiComponentImpl @Inject internal constructor(
  private val title: CreationTitle,
  private val presenter: CreationTitlePresenter
) : BaseUiComponent<CreationTitleUiComponent.Callback>(),
  CreationTitleUiComponent,
  CreationTitlePresenter.Callback {

  override fun id(): Int {
    return title.id()
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      title.teardown()
      presenter.unbind()
    }

    title.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun onSaveState(outState: Bundle) {
    title.saveState(outState)
  }

  override fun onRender(state: NameState, oldState: NameState?) {
    renderName(state, oldState)
    renderError(state, oldState)
  }

  private fun renderName(
    state: NameState,
    oldState: NameState?
  ) {
    state.name.let { name ->
      if (oldState == null || name != oldState.name) {
        val firstUpdate = (oldState == null)
        title.updateName(name, firstUpdate)
      }
    }
  }

  private fun renderError(
    state: NameState,
    oldState: NameState?
  ) {
    state.throwable.let { throwable ->
      if (oldState == null || throwable != oldState.throwable) {
        if (throwable == null) {
          title.clearError()
        } else {
          title.showError(throwable)
        }
      }
    }
  }

}
