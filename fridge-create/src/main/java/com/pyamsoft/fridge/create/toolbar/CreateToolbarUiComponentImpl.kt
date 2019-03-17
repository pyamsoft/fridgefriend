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

package com.pyamsoft.fridge.create.toolbar

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.create.toolbar.CreateToolbarUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.widget.shadow.DropshadowView
import javax.inject.Inject

internal class CreateToolbarUiComponentImpl @Inject internal constructor(
  private val toolbar: CreateToolbar,
  private val dropshadowView: DropshadowView,
  private val presenter: CreateToolbarPresenter
) : BaseUiComponent<CreateToolbarUiComponent.Callback>(),
  CreateToolbarUiComponent,
  CreateToolbarPresenter.Callback {

  override fun id(): Int {
    return toolbar.id()
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      toolbar.teardown()
      dropshadowView.teardown()
      presenter.unbind()
    }

    toolbar.inflate(savedInstanceState)
    dropshadowView.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun saveState(outState: Bundle) {
    toolbar.saveState(outState)
  }

  override fun handleClose() {
    callback.onClose()
  }

  override fun layout(constraintLayout: ConstraintLayout) {
    ConstraintSet().apply {
      clone(constraintLayout)

      toolbar.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      dropshadowView.also {
        connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      applyTo(constraintLayout)
    }
  }

}
