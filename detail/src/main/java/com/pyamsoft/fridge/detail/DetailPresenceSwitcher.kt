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

import android.view.LayoutInflater
import androidx.annotation.CheckResult
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.DetailPresenceSwitchBinding
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiView
import timber.log.Timber
import javax.inject.Inject

class DetailPresenceSwitcher @Inject internal constructor(
    appBarActivity: AppBarActivity,
) : UiView<DetailViewState, DetailViewEvent.SwitcherEvent>() {

    private var _bindingRoot: TabLayout? = null
    private val layoutRoot: TabLayout
        get() = requireNotNull(_bindingRoot)

    init {
        // Replace the app bar background during switcher presence
        doOnInflate {
            appBarActivity.requireAppBar { appBar ->
                val inflater = LayoutInflater.from(appBar.context)
                val binding = DetailPresenceSwitchBinding.inflate(inflater, appBar)
                _bindingRoot = binding.detailPresenceSwitcherRoot.also { onCreate(it) }
            }
        }

        doOnTeardown {
            _bindingRoot?.also { binding ->
                appBarActivity.withAppBar { appBar ->
                    appBar.removeView(binding)
                }

                onDestroy(binding)
            }
        }
    }

    private fun onDestroy(tabs: TabLayout) {
        tabs.removeAllTabs()
    }

    private fun onCreate(tabs: TabLayout) {
        changeBackground(tabs)
        addTabs(tabs)
        attachListener(tabs)
    }

    private fun attachListener(tabs: TabLayout) {
        val listener = object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                val presence = getTabPresence(tab) ?: return
                publish(DetailViewEvent.SwitcherEvent.PresenceSwitched(presence))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }

        }

        tabs.apply {
            addOnTabSelectedListener(listener)
            doOnTeardown {
                removeOnTabSelectedListener(listener)
            }
        }
    }

    private fun addTabs(tabs: TabLayout) {
        tabs.apply {
            addTab(newTab().setText("NEED").setTag(FridgeItem.Presence.NEED))
            addTab(newTab().setText("HAVE").setTag(FridgeItem.Presence.HAVE))
        }
    }

    private fun changeBackground(tabs: TabLayout) {
        tabs.isVisible = true
    }

    override fun render(state: UiRender<DetailViewState>) {
        state.mapChanged { it.listItemPresence }.render(viewScope) { handlePresence(it) }
    }

    @CheckResult
    private fun getTabPresence(tab: TabLayout.Tab): FridgeItem.Presence? {
        val tag = tab.tag
        if (tag == null) {
            Timber.w("No tag found on tab: $tab")
            return null
        }

        if (tag !is FridgeItem.Presence) {
            Timber.w("Tag is not Presence model: $tag")
            return null
        }

        return tag
    }

    private fun handlePresence(presence: FridgeItem.Presence) {
        val tabs = layoutRoot
        for (i in 0 until tabs.tabCount) {
            val tab = tabs.getTabAt(i)
            if (tab == null) {
                Timber.w("No tab found at index: $i")
                continue
            }

            val tag = getTabPresence(tab)
            if (tag == presence) {
                tabs.selectTab(tab, true)
                break
            }
        }
    }
}
