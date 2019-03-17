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
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.Injector
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.detail.title.DetailTitleUiComponent
import com.pyamsoft.fridge.detail.toolbar.DetailToolbarUiComponent
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import javax.inject.Inject

internal class EntryCreateFragment : Fragment(),
  DetailToolbarUiComponent.Callback,
  DetailTitleUiComponent.Callback {

  @field:Inject internal lateinit var toolbar: DetailToolbarUiComponent
  @field:Inject internal lateinit var title: DetailTitleUiComponent

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.layout_constraint, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val parent = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
      .plusDetailComponent()
      .toolbarActivity(requireToolbarActivity())
      .parent(parent)
      .build()
      .inject(this)

    title.bind(viewLifecycleOwner, savedInstanceState, this)
    toolbar.bind(viewLifecycleOwner, savedInstanceState, this)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    title.saveState(outState)
    toolbar.saveState(outState)
  }

  override fun onBack() {
    requireActivity().onBackPressed()
  }

  companion object {

    const val TAG = "EntryCreateDialog"

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return EntryCreateFragment().apply {
        arguments = Bundle().apply {
        }
      }
    }
  }

}
