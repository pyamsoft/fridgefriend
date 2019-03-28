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

package com.pyamsoft.fridge.detail.list.item

import android.view.View
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.list.DetailListInteractor
import com.pyamsoft.fridge.detail.list.item.add.AddNewItemPresenter
import com.pyamsoft.fridge.detail.list.item.add.AddNewItemUiComponent
import com.pyamsoft.fridge.detail.list.item.add.AddNewItemUiComponentImpl
import com.pyamsoft.fridge.detail.list.item.add.AddNewItemView
import com.pyamsoft.fridge.detail.list.item.fridge.DetailItemPresenter
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemUiComponent
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemUiComponentImpl
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

// This Factory exists because Dagger does not let me apply a different scope within a scope
// and I need a separate scope for each list item
//
// This is ugly, and bad DI
// But I don't understand how to multi-map stuff with Dagger, so here we are
internal class DetailItemUiComponentFactory @Inject internal constructor(
  @Named("detail_entry_id") private val entryId: String,
  private val nonPersistedStateMap: MutableMap<String, Int>,
  private val interactor: DetailListInteractor,
  private val theming: Theming,
  private val imageLoader: ImageLoader
) {

  init {
    Timber.d("New DetailItemUiComponentFactory")
  }

  @CheckResult
  fun createItem(parent: View, item: FridgeItem): DetailListItemUiComponent {
    // This is ugly, and bad DI
    // But I don't understand how to multi-map stuff with Dagger, so here we are
    val presenter = DetailItemPresenter(interactor)
    val view = DetailListItemView(
      item,
      parent,
      entryId,
      theming,
      imageLoader,
      nonPersistedStateMap,
      presenter
    )
    return DetailListItemUiComponentImpl(view, presenter)
  }

  @CheckResult
  fun createAddNewItem(parent: View): AddNewItemUiComponent {
    // This is ugly, and bad DI
    // But I don't understand how to multi-map stuff with Dagger, so here we are
    val presenter = AddNewItemPresenter()
    val view = AddNewItemView(parent, theming, imageLoader, presenter)
    return AddNewItemUiComponentImpl(view, presenter)
  }

}