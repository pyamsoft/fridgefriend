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
 */

package com.pyamsoft.fridge.detail

import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.annotation.CheckResult
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.core.view.iterator
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import timber.log.Timber
import javax.inject.Inject

class DetailToolbar @Inject internal constructor(
    toolbarActivity: ToolbarActivity
) : UiView<DetailViewState, DetailViewEvent>() {

    private val groupIdSearch = View.generateViewId()
    private val groupIdSubmenu = View.generateViewId()
    private val itemIdSearch = View.generateViewId()
    private val itemIdSubmenu = View.generateViewId()
    private val itemIdTitle = View.generateViewId()
    private val itemIdCreatedDate = View.generateViewId()
    private val itemIdName = View.generateViewId()
    private val itemIdPurchasedDate = View.generateViewId()
    private val itemIdExpirationDate = View.generateViewId()

    private var subMenu: SubMenu? = null
    private var searchItem: MenuItem? = null

    private val publishHandler = Handler(Looper.getMainLooper())

    // NOTE(Peter): Hack because Android does not allow us to use Controlled view components like
    // React does by binding input and drawing to the render loop.
    //
    // This initialRenderPerformed variable allows us to set the initial state of a view once, and bind listeners to
    // it because the state.item is only available in render instead of inflate. Once the firstRender
    // has set the view component up, the actual input will no longer be tracked via state render events,
    // so the input is uncontrolled.
    private var initialRenderPerformed = false

    init {
        doOnInflate {
            toolbarActivity.requireToolbar { toolbar ->
                toolbar.setUpEnabled(true)
                toolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
                    publish(DetailViewEvent.Back)
                })
                initializeMenuItems(toolbar)
            }
        }

        doOnTeardown {
            toolbarActivity.withToolbar { toolbar ->
                toolbar.setUpEnabled(false)
                toolbar.teardownMenu()
            }
        }
    }

    private fun initializeMenuItems(toolbar: Toolbar) {
        toolbar.doOnLayout {
            subMenu = toolbar.initSubmenu()
            searchItem = toolbar.initSearchItem()
        }
    }

    @CheckResult
    private fun sortForMenuItem(
        item: MenuItem,
        showExtra: Boolean
    ): DetailViewState.Sorts? {
        return when (item.itemId) {
            itemIdCreatedDate -> DetailViewState.Sorts.CREATED
            itemIdName -> DetailViewState.Sorts.NAME
            itemIdPurchasedDate -> if (showExtra) DetailViewState.Sorts.PURCHASED else null
            itemIdExpirationDate -> if (showExtra) DetailViewState.Sorts.EXPIRATION else null
            else -> null
        }
    }

    private fun handleSubmenu(state: DetailViewState) {
        subMenu?.let { subMenu ->
            val isHavePresence = state.listItemPresence == FridgeItem.Presence.HAVE
            val showExtraMenuItems = isHavePresence && searchItem?.isActionViewExpanded == false

            subMenu.findItem(itemIdPurchasedDate)?.isVisible = showExtraMenuItems
            subMenu.findItem(itemIdExpirationDate)?.isVisible = showExtraMenuItems

            val currentSort = state.sort
            subMenu.forEach { item ->
                sortForMenuItem(item, showExtraMenuItems)?.also { sort ->
                    if (currentSort == sort) {
                        item.isChecked = true
                    }
                }
            }

            // If nothing is checked, thats a no no
            if (subMenu.children.all { !it.isChecked }) {
                Timber.w("SORTS: NOTHING IS CHECKED: $currentSort")
            }
        }
    }

    override fun render(state: DetailViewState) {
        handleSubmenu(state)
        handleInitialSearch(state)
    }

    private fun handleInitialSearch(state: DetailViewState) {
        if (initialRenderPerformed) {
            return
        }

        val item = searchItem ?: return
        val searchView = item.actionView as? SearchView ?: return

        initialRenderPerformed = true

        state.search.let { search ->
            if (search.isNotBlank()) {
                if (item.expandActionView()) {
                    searchView.setQuery(search, true)
                }
            }
        }
    }

    private fun debouncedPublish(event: DetailViewEvent) {
        publishHandler.removeCallbacksAndMessages(null)
        publishHandler.postDelayed({ publish(event) }, SEARCH_PUBLISH_TIMEOUT)
    }

    private fun Toolbar.setVisibilityOfNonSearchItems(visible: Boolean) {
        for (item in this.menu) {
            if (item.itemId != itemIdSearch) {
                item.isVisible = visible
            }
        }
    }

    @CheckResult
    private fun Toolbar.initSearchItem(): MenuItem {
        val toolbar = this
        return this.menu.add(groupIdSearch, itemIdSearch, Menu.NONE, "Search").apply {
            setIcon(R.drawable.ic_search_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {

                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    // Need to post so this fires after all other UI work in toolbar
                    toolbar.post { setVisibilityOfNonSearchItems(false) }
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    // Need to post so this fires after all other UI work in toolbar
                    toolbar.post { setVisibilityOfNonSearchItems(true) }
                    return true
                }
            })
            actionView = SearchView(toolbar.context).apply {
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
                        debouncedPublish(DetailViewEvent.SearchQuery(query))
                        return true
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        debouncedPublish(DetailViewEvent.SearchQuery(newText))
                        return true
                    }
                })
            }
        }
    }

    @CheckResult
    private fun Toolbar.initSubmenu(): SubMenu {
        return this.menu.addSubMenu(groupIdSubmenu, itemIdSubmenu, Menu.NONE, "Sorts")
            .also { subMenu ->
                subMenu.item.setIcon(R.drawable.ic_sort_24dp)
                subMenu.item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                subMenu.add(Menu.NONE, itemIdTitle, Menu.NONE, "").apply {
                    title = buildSpannedString { bold { append("Sorts") } }
                }
                subMenu.add(groupIdSubmenu, itemIdCreatedDate, 1, "Created Date").apply {
                    isChecked = false
                    setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.CREATED))
                }
                subMenu.add(groupIdSubmenu, itemIdName, 2, "Name").apply {
                    isChecked = false
                    setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.NAME))
                }
                subMenu.add(groupIdSubmenu, itemIdPurchasedDate, 3, "Purchase Date").apply {
                    isChecked = false
                    setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.PURCHASED))
                }
                subMenu.add(groupIdSubmenu, itemIdExpirationDate, 4, "Expiration Date").apply {
                    isChecked = false
                    setOnMenuItemClickListener(clickListener(DetailViewState.Sorts.EXPIRATION))
                }

                subMenu.setGroupCheckable(groupIdSubmenu, true, true)
            }
    }

    private fun Toolbar.teardownMenu() {
        handler?.removeCallbacksAndMessages(null)
        publishHandler.removeCallbacksAndMessages(null)
        setVisibilityOfNonSearchItems(true)
        teardownSubmenu()
        teardownSearch()
    }

    private fun Toolbar.teardownSearch() {
        this.menu.removeGroup(groupIdSearch)
        searchItem = null
    }

    private fun Toolbar.teardownSubmenu() {
        this.menu.removeGroup(groupIdSubmenu)
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
        private const val SEARCH_PUBLISH_TIMEOUT = 400L
    }
}
