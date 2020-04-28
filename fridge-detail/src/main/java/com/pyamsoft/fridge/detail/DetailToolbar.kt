/*
 * Copyright 2020 Peter Kenji Yamanaka
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

import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.annotation.CheckResult
import androidx.appcompat.widget.Toolbar
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.forEach
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiBundleReader
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import javax.inject.Inject

class DetailToolbar @Inject internal constructor(
    toolbarActivity: ToolbarActivity,
    presence: FridgeItem.Presence
) : UiView<DetailViewState, DetailViewEvent>() {

    private var subMenu: SubMenu? = null

    init {
        doOnInflate {
            toolbarActivity.requireToolbar { toolbar ->
                initializeMenuItems(toolbar, presence)
            }
        }

        doOnTeardown {
            toolbarActivity.withToolbar { toolbar ->
                toolbar.teardown()
            }
        }
    }

    private fun initializeMenuItems(
        toolbar: Toolbar,
        presence: FridgeItem.Presence
    ) {
        subMenu = toolbar.initMenu(presence)
    }

    override fun onInit(savedInstanceState: UiBundleReader) {
    }

    override fun render(state: DetailViewState) {
        subMenu?.let { subMenu ->
            val currentSort = state.sort
            subMenu.forEach { item ->
                val expectedSort = when (item.itemId) {
                    ID_CREATED_DATE -> DetailViewState.Sorts.CREATED
                    ID_NAME -> DetailViewState.Sorts.NAME
                    ID_PURCHASED_DATE -> DetailViewState.Sorts.PURCHASED
                    ID_EXPIRATION_DATE -> DetailViewState.Sorts.EXPIRATION
                    else -> return@forEach
                }
                if (currentSort == expectedSort) {
                    item.isChecked = true
                }
            }
        }
    }

    @CheckResult
    private fun Toolbar.initMenu(presence: FridgeItem.Presence): SubMenu {
        return this.menu.addSubMenu(GROUP_ID, ID_SUBMENU, Menu.NONE, "Sorts").also { subMenu ->
            subMenu.item.setIcon(R.drawable.ic_sort_24dp)
            subMenu.item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            subMenu.add(Menu.NONE, ID_TITLE, Menu.NONE, "").apply {
                title = buildSpannedString { bold { append("Sorts") } }
            }
            subMenu.add(GROUP_ID, ID_CREATED_DATE, 1, "Created Date").apply {
                isChecked = false
                setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.CREATED))
            }
            subMenu.add(GROUP_ID, ID_NAME, 2, "Name").apply {
                isChecked = false
                setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.NAME))
            }
            if (presence == FridgeItem.Presence.HAVE) {
                subMenu.add(GROUP_ID, ID_PURCHASED_DATE, 3, "Purchase Date").apply {
                    isChecked = false
                    setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.PURCHASED))
                }
                subMenu.add(GROUP_ID, ID_EXPIRATION_DATE, 4, "Expiration Date").apply {
                    isChecked = false
                    setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.EXPIRATION))
                }
            }
            subMenu.setGroupCheckable(GROUP_ID, true, true)
        }
    }

    private fun Toolbar.teardown() {
        subMenu?.let { subMenu ->
            // Go backwards to avoid index oob error
            for (index in subMenu.size() until 0) {
                val item = subMenu.getItem(index)
                item.setOnMenuItemClickListener(null)
                subMenu.removeItem(item.itemId)
            }
            menu.removeItem(subMenu.item.itemId)
        }
        subMenu = null
    }

    @CheckResult
    private fun clickListener(sort: DetailViewState.Sorts): MenuItem.OnMenuItemClickListener {
        return MenuItem.OnMenuItemClickListener {
            publish(DetailViewEvent.ChangeSort(sort))
            return@OnMenuItemClickListener true
        }
    }

    companion object {
        private const val GROUP_ID = 69
        private const val ID_SUBMENU = 420
        private const val ID_TITLE = 100
        private const val ID_CREATED_DATE = 101
        private const val ID_NAME = 102
        private const val ID_PURCHASED_DATE = 103
        private const val ID_EXPIRATION_DATE = 104
    }
}
