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

package com.pyamsoft.fridge.ui

import android.view.View
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import com.pyamsoft.pydroid.util.doOnDestroy
import timber.log.Timber

private inline fun watchToolbarOffset(
    view: View,
    owner: LifecycleOwner,
    crossinline onNewMargin: (Int) -> Unit,
) {
    view.doOnApplyWindowInsets(owner) { _, insets, _ ->
        val toolbarTopMargin = insets.systemWindowInsetTop
        onNewMargin(toolbarTopMargin)
    }
}

private inline fun watchAppBarHeight(
    appBar: View,
    owner: LifecycleOwner,
    crossinline onNewHeight: (Int) -> Unit,
) {
    val listener = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
        onNewHeight(v.height)
    }
    appBar.addOnLayoutChangeListener(listener)
    owner.doOnDestroy { appBar.removeOnLayoutChangeListener(listener) }
}

private fun applyNewViewOffset(
    view: View,
    initialTopPadding: Int,
    offset: Int?,
    appBarHeight: Int?,
) {
    if (offset == null) {
        return
    }

    if (appBarHeight == null) {
        return
    }

    val newPadding = initialTopPadding + offset + appBarHeight
    Timber.d("Apply new offset padding: $view $newPadding")
    view.updatePadding(top = newPadding)
}

fun View.applyToolbarOffset(appBarActivity: AppBarActivity, owner: LifecycleOwner) {
    val initialTopPadding = this.paddingTop
    appBarActivity.withAppBar { appBar ->

        // Keep track off last seen values here
        var lastOffset: Int? = null
        var lastHeight: Int? = null

        watchAppBarHeight(appBar, owner) { newHeight ->
            lastHeight = newHeight
            applyNewViewOffset(this, initialTopPadding, lastOffset, lastHeight)
        }

        watchToolbarOffset(this, owner) { newOffset ->
            lastOffset = newOffset
            applyNewViewOffset(this, initialTopPadding, lastOffset, lastHeight)
        }
    }
}
