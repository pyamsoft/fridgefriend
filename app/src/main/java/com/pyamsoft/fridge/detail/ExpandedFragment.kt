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

package com.pyamsoft.fridge.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.expand.ExpandUiComponent
import com.pyamsoft.pydroid.arch.layout
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireArguments
import javax.inject.Inject

class ExpandedFragment : Fragment() {

  @JvmField @Inject internal var component: ExpandUiComponent? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.layout_constraint, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val parent = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
    val item = requireNotNull(requireArguments().getParcelable<JsonMappableFridgeItem>(ITEM))
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusExpandComponent()
        .create(parent, item)
        .inject(this)

    val component = requireNotNull(component)
    component.bind(parent, viewLifecycleOwner, savedInstanceState, Unit)

    parent.layout {
      component.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    component?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    component = null
  }

  companion object {

    const val TAG = "ExpandedFragment"
    private const val ITEM = "item"

    @JvmStatic
    @CheckResult
    fun newInstance(
      item: FridgeItem
    ): Fragment {
      return ExpandedFragment().apply {
        arguments = Bundle().apply {
          putParcelable(ITEM, JsonMappableFridgeItem.from(item))
        }
      }
    }
  }

}
