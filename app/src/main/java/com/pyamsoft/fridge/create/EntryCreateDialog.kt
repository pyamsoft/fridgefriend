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

package com.pyamsoft.fridge.create

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.Injector
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.create.title.CreateTitleUiComponent
import com.pyamsoft.fridge.create.toolbar.CreateToolbarUiComponent
import javax.inject.Inject

internal class EntryCreateDialog : DialogFragment(),
  CreateToolbarUiComponent.Callback,
  CreateTitleUiComponent.Callback {

  @field:Inject internal lateinit var toolbar: CreateToolbarUiComponent
  @field:Inject internal lateinit var title: CreateTitleUiComponent

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
      .plusCreateComponent()
      .parent(parent)
      .build()
      .inject(this)

    setDialogBackground()
    title.bind(viewLifecycleOwner, savedInstanceState, this)
    toolbar.bind(viewLifecycleOwner, savedInstanceState, this)

    toolbar.layout(parent)
    title.layout(parent, toolbar.id())
  }

  private fun setDialogBackground() {
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    title.saveState(outState)
    toolbar.saveState(outState)
  }

  override fun onResume() {
    super.onResume()
    dialog.window?.apply {
      setLayout(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setGravity(Gravity.CENTER)
    }
  }

  override fun onClose() {
    dismiss()
  }

  companion object {

    const val TAG = "EntryCreateDialog"

    @JvmStatic
    @CheckResult
    fun newInstance(): DialogFragment {
      return EntryCreateDialog().apply {
        arguments = Bundle().apply {
        }
      }
    }
  }

}
