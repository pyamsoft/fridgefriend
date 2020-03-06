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

package com.pyamsoft.fridge.detail.base

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.databinding.DetailListItemDateBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.util.tintWith
import java.util.Calendar
import timber.log.Timber

abstract class BaseItemDate<S : UiViewState, V : UiViewEvent> protected constructor(
    private val imageLoader: ImageLoader,
    private val theming: ThemeProvider,
    parent: ViewGroup
) : BindingUiView<S, V, DetailListItemDateBinding>(parent) {

    final override val viewBinding = DetailListItemDateBinding::inflate

    final override val layoutRoot by boundView { detailItemDate }

    private var dateLoaded: Loaded? = null

    init {
        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        dateLoaded?.dispose()
        dateLoaded = null
        binding.detailItemDateText.text = ""
    }

    protected fun baseRender(item: FridgeItem?) {
        if (item == null) {
            clear()
        } else {
            val expireTime = item.expireTime()
            if (expireTime != null) {
                val date = Calendar.getInstance()
                    .apply { time = expireTime }
                Timber.d("Expire time is: $date")

                // Month is zero indexed in storage
                val month = date.get(Calendar.MONTH)
                val day = date.get(Calendar.DAY_OF_MONTH)
                val year = date.get(Calendar.YEAR)

                val dateString =
                    "${"${month + 1}".padStart(2, '0')}/${
                    "$day".padStart(2, '0')}/${
                    "$year".padStart(4, '0')}"
                binding.detailItemDateText.text = dateString
                binding.detailItemDateIcon.isVisible = false
            } else {
                binding.detailItemDateText.text = "-----"
                binding.detailItemDateIcon.isVisible = true

                dateLoaded?.dispose()
                dateLoaded = imageLoader.load(R.drawable.ic_date_range_24dp)
                    .mutate {
                        val color = if (theming.isDarkTheme()) R.color.white else R.color.black
                        it.tintWith(layoutRoot.context, color)
                    }
                    .into(binding.detailItemDateIcon)
            }
        }
    }
}
