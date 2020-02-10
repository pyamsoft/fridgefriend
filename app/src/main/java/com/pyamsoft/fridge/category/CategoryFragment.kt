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

package com.pyamsoft.fridge.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.main.SnackbarContainer
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import javax.inject.Inject

internal class CategoryFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var list: CategoryListView? = null

    @JvmField
    @Inject
    internal var heroImage: CategoryHeroImage? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by factory<CategoryViewModel> { factory }

    private var stateSaver: StateSaver? = null

    private var rootView: CoordinatorLayout? = null

    override fun getSnackbarContainer(): CoordinatorLayout? {
        return rootView
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_coordinator, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val parent = view.findViewById<CoordinatorLayout>(R.id.layout_coordinator)
        rootView = parent

        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusCategoryComponent()
            .create(
                requireActivity(),
                parent,
                viewLifecycleOwner
            )
            .inject(this)

        val list = requireNotNull(list)
        val heroImage = requireNotNull(heroImage)
        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            heroImage,
            list
        ) {
            // TODO
        }

        initializeApp()
    }

    private fun initializeApp() {
        val act = requireActivity()
        if (act is VersionChecker) {
            act.checkVersionForUpdate()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        rootView = null
        stateSaver = null
        factory = null
    }

    companion object {

        const val TAG = "CategoryFragment"

        @JvmStatic
        @CheckResult
        fun newInstance(): Fragment {
            return CategoryFragment().apply {
                arguments = Bundle().apply {}
            }
        }
    }
}
