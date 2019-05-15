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
import android.widget.FrameLayout
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.list.DetailListControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.shop.list.ShoppingList
import com.pyamsoft.fridge.detail.shop.list.ShoppingListViewModel
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbar
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarControllerEvent.NavigateUp
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarViewModel
import com.pyamsoft.pydroid.arch.impl.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import timber.log.Timber
import javax.inject.Inject

internal class ShoppingFragment : Fragment() {

  @JvmField @Inject internal var toolbarViewModel: ShoppingToolbarViewModel? = null
  @JvmField @Inject internal var toolbar: ShoppingToolbar? = null

  @JvmField @Inject internal var listViewModel: ShoppingListViewModel? = null
  @JvmField @Inject internal var list: ShoppingList? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.layout_frame, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val parent = view.findViewById<FrameLayout>(R.id.layout_frame)
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusDetailComponent()
        .create(requireToolbarActivity(), parent)
        .plusShoppingComponent()
        .create(viewLifecycleOwner)
        .inject(this)

    val list = requireNotNull(list)
    val toolbar = requireNotNull(toolbar)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(listViewModel), list
    ) {
      return@createComponent when (it) {
        is ExpandForEditing -> expandItem(it.item)
      }
    }

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(toolbarViewModel), toolbar
    ) {
      return@createComponent when (it) {
        is NavigateUp -> close()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    list?.saveState(outState)
    toolbar?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    list = null
    toolbar = null
  }

  private fun close() {
    requireActivity().onBackPressed()
  }

  private fun expandItem(item: FridgeItem) {
    Timber.d("NOOP for shopping expand: $item")
  }

  companion object {

    const val TAG = "ShoppingFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return ShoppingFragment().apply {
        arguments = Bundle().apply {
        }
      }
    }
  }

}
