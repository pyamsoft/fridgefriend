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

package com.pyamsoft.fridge.detail

import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.fridge.ui.chart.Pie
import com.pyamsoft.fridge.ui.view.UiHeroImage
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class DetailHeroImage @Inject internal constructor(
    parent: ViewGroup,
    imageLoader: ImageLoader,
    owner: LifecycleOwner,
    appBarActivity: AppBarActivity,
) : UiHeroImage<DetailViewState, Nothing>(parent, owner, appBarActivity, imageLoader) {

    private var pie: Pie? = null

    init {
        doOnInflate {
            pie = Pie.fromChart(binding.coreHeroPie)
        }

        doOnTeardown {
            pie?.clear()
            pie = null
        }
    }

    override fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: DetailViewState,
    ): Loaded {
        val need = state.listItemPresence == FridgeItem.Presence.NEED
        val icon = if (need) R.drawable.bg_item_need else R.drawable.bg_item_have
        return imageLoader.load(icon).into(imageView)
    }

    override fun onAdditionalRender(state: UiRender<DetailViewState>) {
        state.mapChanged { it.allItems }.render(viewScope) { handleItems(it) }
        state.mapChanged { it.showing }.render(viewScope) { handleDescription(it) }
        state.mapChanged { it.entry }.mapChanged { it?.name().orEmpty() }
            .render(viewScope) { handleTitle(it) }
    }

    private fun handleDescription(showing: DetailViewState.Showing) {
//        binding.coreHeroPie.setDescription(
//            when (showing) {
//                DetailViewState.Showing.FRESH -> "Fresh Items"
//                DetailViewState.Showing.CONSUMED -> "Consumed Items"
//                DetailViewState.Showing.SPOILED -> "Spoiled Items"
//            }
//        )
    }

    private fun handleTitle(name: String) {
//        binding.coreHeroPie.setTitle(name)
    }

    private fun handleItems(items: List<FridgeItem>) {
        var freshItems = 0
        var consumedItems = 0
        var spoiledItems = 0

        items.groupBy { item ->
            return@groupBy when {
                item.isSpoiled() -> GROUP_SPOILED
                item.isConsumed() -> GROUP_CONSUMED
                else -> GROUP_FRESH
            }
        }.forEach { entry ->
            when {
                entry.key === GROUP_SPOILED -> ++spoiledItems
                entry.key === GROUP_CONSUMED -> ++consumedItems
                else -> ++freshItems
            }
        }

        val freshData = Pie.Data(
            value = freshItems.toFloat(),
            color = Color.parseColor("#00FF00")
        )

        val consumedData = Pie.Data(
            value = consumedItems.toFloat(),
            color = Color.parseColor("#0000FF")
        )

        val spoiledData = Pie.Data(
            value = spoiledItems.toFloat(),
            color = Color.parseColor("#FF0000")
        )

        requireNotNull(pie).setData(listOf(freshData, consumedData, spoiledData))
    }

    companion object {

        private const val GROUP_FRESH = "Fresh"
        private const val GROUP_SPOILED = "Spoiled"
        private const val GROUP_CONSUMED = "Consumed"
    }
}
