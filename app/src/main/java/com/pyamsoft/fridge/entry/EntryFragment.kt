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

package com.pyamsoft.fridge.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.commitNow
import timber.log.Timber
import javax.inject.Inject

internal class EntryFragment : Fragment() {

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by viewModelFactory<EntryViewModel> { factory }

    private var stateSaver: StateSaver? = null

    private var fragmentContainerId = 0

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
        val binding = LayoutCoordinatorBinding.bind(view)
        fragmentContainerId = binding.layoutCoordinator.id
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusEntryComponent()
            .create(binding.layoutCoordinator)
            .inject(this)

        stateSaver = createComponent(
            savedInstanceState, viewLifecycleOwner,
            viewModel
        ) {
            return@createComponent when (it) {
                is EntryControllerEvent.LoadEntry -> pushPage(it.entry)
            }
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

        factory = null
        stateSaver = null
    }

    private fun pushPage(entry: FridgeEntry) {
        val tag = entry.id().id
        Timber.d("Push new entry page: $tag")
        val fm = childFragmentManager
        if (fm.findFragmentByTag(tag) == null) {
            val presenceString = requireNotNull(requireArguments().getString(EXTRA_PRESENCE))
            val presence = Presence.valueOf(presenceString)
            fm.commitNow(viewLifecycleOwner) {
                replace(
                    fragmentContainerId,
                    DetailFragment.newInstance(entry, presence),
                    tag
                )
            }
        }
    }

    companion object {

        const val EXTRA_PRESENCE = "presence"
        const val TAG = "EntryFragment"

        @JvmStatic
        @CheckResult
        fun newInstance(presence: Presence): Fragment {
            return EntryFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_PRESENCE, presence.name)
                }
            }
        }
    }
}
