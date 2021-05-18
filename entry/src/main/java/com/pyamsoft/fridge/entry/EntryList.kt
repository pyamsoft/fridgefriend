/*
 * Copyright 2021 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.entry

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.ItemTouchHelper
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import com.pyamsoft.fridge.entry.item.EntryItemViewHolder
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject
import timber.log.Timber

class EntryList
@Inject
internal constructor(
    private val theming: ThemeProvider,
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
    factory: EntryItemComponent.Factory,
) : BaseEntryList<EntryViewEvent.ListEvents>(parent, factory) {

  private var touchHelper: ItemTouchHelper? = null

  private var leftBehindLoaded: Loaded? = null
  private var leftBehindDrawable: Drawable? = null

  private var rightBehindLoaded: Loaded? = null
  private var rightBehindDrawable: Drawable? = null

  init {
    doOnInflate { setupSwipeCallback() }

    doOnTeardown {
      touchHelper?.attachToRecyclerView(null)
      touchHelper = null
    }

    doOnTeardown { clearLoaded() }
  }

  private fun clearLoaded() {
    leftBehindLoaded?.dispose()
    leftBehindLoaded = null

    rightBehindLoaded?.dispose()
    rightBehindLoaded = null

    leftBehindDrawable = null
    rightBehindDrawable = null
  }

  private fun deleteListItem(position: Int) {
    publish(EntryViewEvent.ListEvents.DeleteEntry(position))
  }

  private fun setupSwipeCallback() {
    applySwipeCallback(ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) { position, _ ->
      val holder = binding.entryList.findViewHolderForAdapterPosition(position)
      if (holder == null) {
        Timber.w("ViewHolder is null, cannot respond to swipe")
        return@applySwipeCallback
      }
      if (holder !is EntryItemViewHolder) {
        Timber.w("ViewHolder is not EntryItemViewHolder, cannot respond to swipe")
        return@applySwipeCallback
      }

      deleteListItem(position)
    }
  }

  @CheckResult
  private fun Drawable.themeIcon(): Drawable {
    val color =
        if (theming.isDarkTheme()) com.pyamsoft.pydroid.ui.R.color.white
        else com.pyamsoft.pydroid.ui.R.color.black
    return this.tintWith(layoutRoot.context, color)
  }

  private inline fun createSwipeCallback(
      directions: Int,
      crossinline itemSwipeCallback: (position: Int, directions: Int) -> Unit,
  ) {
    val left = leftBehindDrawable
    val right = rightBehindDrawable
    if (left == null || right == null) {
      return
    }

    val cb =
        object : SimpleSwipeCallback.ItemSwipeCallback {

          override fun itemSwiped(position: Int, direction: Int) {
            itemSwipeCallback(position, direction)
          }
        }

    val swipeCallback =
        SimpleSwipeCallback(cb, left, directions, Color.TRANSPARENT).apply {
          withBackgroundSwipeRight(Color.TRANSPARENT)
          withLeaveBehindSwipeRight(right)
        }

    // Detach any existing helper from the recyclerview
    touchHelper?.attachToRecyclerView(null)

    // Attach new helper
    val helper = ItemTouchHelper(swipeCallback).apply { attachToRecyclerView(binding.entryList) }

    // Set helper for cleanup later
    touchHelper = helper
  }

  private inline fun applySwipeCallback(
      directions: Int,
      crossinline itemSwipeCallback: (position: Int, directions: Int) -> Unit,
  ) {

    clearLoaded()
    leftBehindLoaded =
        imageLoader
            .asDrawable()
            .load(R.drawable.ic_delete_24dp)
            .mutate { it.themeIcon() }
            .into(
                object : ImageTarget<Drawable> {
                  override fun clear() {
                    // Does nothing on its own, clear the touch helper to free
                  }

                  override fun setImage(image: Drawable) {
                    leftBehindDrawable = image
                    createSwipeCallback(directions, itemSwipeCallback)
                  }
                })

    rightBehindLoaded =
        imageLoader
            .asDrawable()
            .load(R.drawable.ic_delete_24dp)
            .mutate { it.themeIcon() }
            .into(
                object : ImageTarget<Drawable> {
                  override fun clear() {
                    // Does nothing on its own, clear the touch helper to free
                  }

                  override fun setImage(image: Drawable) {
                    rightBehindDrawable = image
                    createSwipeCallback(directions, itemSwipeCallback)
                  }
                })
  }

  override fun onRefresh() {
    publish(EntryViewEvent.ListEvents.ForceRefresh)
  }

  override fun onClick(index: Int) {
    publish(EntryViewEvent.ListEvents.SelectEntry(index))
  }

  override fun onLongPress(index: Int) {
    publish(EntryViewEvent.ListEvents.EditEntry(index))
  }
}
