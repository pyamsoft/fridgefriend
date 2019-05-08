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

package com.pyamsoft.fridge.detail.expand

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.item.fridge.DetailItemFrame
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewModel
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemDate
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemName
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemPresence
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import timber.log.Timber
import javax.inject.Inject

internal class ExpandUiComponentImpl @Inject internal constructor(
  private val frame: DetailItemFrame,
  private val name: DetailListItemName,
  private val expireTime: DetailListItemDate,
  private val presence: DetailListItemPresence,
  private val viewModel: DetailItemViewModel
) : BaseUiComponent<Unit>(), ExpandUiComponent {

  override fun id(): Int {
    return frame.id()
  }

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: Unit
  ) {
    owner.doOnDestroy {
      Timber.w("unbind")
      frame.teardown()
      name.teardown()
      expireTime.teardown()
      presence.teardown()
      viewModel.unbind()
    }

    Timber.d("bind")
    presence.inflate(savedInstanceState)
    expireTime.inflate(savedInstanceState)
    name.inflate(savedInstanceState)
    frame.inflate(savedInstanceState)
    viewModel.bind { state, oldState ->
      // TODO
    }
  }

  override fun onLayout(set: ConstraintSet) {
    presence.also {
      set.connect(it.id(), ConstraintSet.TOP, frame.id(), ConstraintSet.TOP)
      set.connect(it.id(), ConstraintSet.BOTTOM, frame.id(), ConstraintSet.BOTTOM)
      set.connect(it.id(), ConstraintSet.START, frame.id(), ConstraintSet.START)
      set.constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
    }

    expireTime.also {
      set.connect(it.id(), ConstraintSet.TOP, frame.id(), ConstraintSet.TOP)
      set.connect(it.id(), ConstraintSet.BOTTOM, frame.id(), ConstraintSet.BOTTOM)
      set.connect(it.id(), ConstraintSet.END, frame.id(), ConstraintSet.END)
      set.constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
    }

    name.also {
      set.connect(it.id(), ConstraintSet.TOP, frame.id(), ConstraintSet.TOP)
      set.connect(it.id(), ConstraintSet.BOTTOM, frame.id(), ConstraintSet.BOTTOM)
      set.connect(it.id(), ConstraintSet.START, presence.id(), ConstraintSet.END)
      set.connect(it.id(), ConstraintSet.END, expireTime.id(), ConstraintSet.START)
      set.constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
    }
  }

  override fun onSaveState(outState: Bundle) {
    frame.saveState(outState)
    expireTime.saveState(outState)
    presence.saveState(outState)
    name.saveState(outState)
  }

}
