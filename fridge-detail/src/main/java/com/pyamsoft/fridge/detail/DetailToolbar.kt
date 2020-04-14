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

import android.os.Handler
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiBundleReader
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import timber.log.Timber
import javax.inject.Inject

class DetailToolbar @Inject internal constructor(
    toolbarActivity: ToolbarActivity,
    presence: FridgeItem.Presence
) : UiView<DetailViewState, DetailViewEvent>() {

    private val lazyHandler = lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
    private val handler by lazyHandler

    private var searchView: SearchView? = null

    init {
        doOnInflate { savedInstanceState ->
            toolbarActivity.requireToolbar { toolbar ->
                searchView = toolbar.initSearchView(presence).apply {
                    Timber.d("Apply listener: ${this.id} $presence")
                    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            Timber.d("Query submit: $query")
                            debouncedPublish { DetailViewEvent.SearchQuery(query) }
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            Timber.d("Query change: $newText")
                            debouncedPublish { DetailViewEvent.SearchQuery(newText) }
                            return true
                        }
                    })

                    savedInstanceState.useIfAvailable<CharSequence>(KEY_QUERY) { query ->
                        if (query.isNotBlank()) {
                            isIconified = false
                            setQuery(query, true)
                        }
                    }
                }
            }
        }

        doOnSaveState { outState ->
            searchView?.apply {
                val query = this.query
                if (query.isNotBlank()) {
                    outState.put(KEY_QUERY, query)
                } else {
                    outState.remove(KEY_QUERY)
                }
            }
        }

        doOnTeardown {
            searchView?.apply {
                Timber.d("Clear listener: ${this.id} $presence")
                setOnQueryTextListener(null)
            }
            searchView = null

            toolbarActivity.withToolbar { toolbar ->
                toolbar.removeSearch(presence)
            }
        }

        doOnTeardown {
            if (lazyHandler.isInitialized()) {
                handler.removeCallbacksAndMessages(null)
            }
        }
    }

    private inline fun debouncedPublish(crossinline event: () -> DetailViewEvent) {
        if (lazyHandler.isInitialized()) {
            handler.removeCallbacksAndMessages(null)
        }
        handler.postDelayed({ publish(event()) }, 300)
    }

    override fun onInit(savedInstanceState: UiBundleReader) {
    }

    override fun render(state: DetailViewState) {
    }

    @CheckResult
    private fun getMenuForPresence(presence: FridgeItem.Presence): MenuData {
        return when (presence) {
            FridgeItem.Presence.HAVE -> MenuData(R.menu.menu_search_have, R.id.menu_search_have)
            FridgeItem.Presence.NEED -> MenuData(R.menu.menu_search_need, R.id.menu_search_need)
        }
    }

    @CheckResult
    private fun Toolbar.initSearchView(presence: FridgeItem.Presence): SearchView {
        val menuData = getMenuForPresence(presence)
        this.inflateMenu(menuData.menu)
        val searchItem = this.menu.findItem(menuData.item)
        return searchItem.actionView as SearchView
    }

    private fun Toolbar.removeSearch(presence: FridgeItem.Presence) {
        val menuData = getMenuForPresence(presence)
        this.menu.removeItem(menuData.item)
    }

    private data class MenuData internal constructor(
        @MenuRes val menu: Int,
        @IdRes val item: Int
    )

    companion object {
        private const val KEY_QUERY = "key_query"
    }
}
