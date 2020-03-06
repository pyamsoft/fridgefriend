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

package com.pyamsoft.fridge.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.fridge.BuildConfig
import com.pyamsoft.fridge.ButlerParameters
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.Notifications
import com.pyamsoft.fridge.category.CategoryFragment
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.EntryFragment
import com.pyamsoft.fridge.initOnAppStart
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.fridge.permission.PermissionFragment
import com.pyamsoft.fridge.setting.SettingsDialog
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.rating.ChangeLogBuilder
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import com.pyamsoft.pydroid.ui.rating.buildChangeLog
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.util.makeWindowSexy
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class MainActivity : RatingActivity(), VersionChecker {

    override val checkForUpdates: Boolean = false

    override val applicationIcon: Int = R.mipmap.ic_launcher

    override val versionName: String = BuildConfig.VERSION_NAME

    override val changeLogTheme: Int = R.style.Theme_Fridge_Dialog

    override val versionCheckTheme: Int = R.style.Theme_Fridge_Dialog

    override val changeLogLines: ChangeLogBuilder = buildChangeLog {
    }

    override val fragmentContainerId: Int
        get() = requireNotNull(container).id()

    override val snackbarRoot: ViewGroup
        get() {
            val containerFragment = supportFragmentManager.findFragmentById(fragmentContainerId)
            if (containerFragment is SnackbarContainer) {
                val snackbarContainer = containerFragment.getSnackbarContainer()
                if (snackbarContainer != null) {
                    return snackbarContainer
                }
            }

            return requireNotNull(rootView)
        }

    private var rootView: ConstraintLayout? = null
    private var stateSaver: StateSaver? = null

    @JvmField
    @Inject
    internal var butler: Butler? = null

    @JvmField
    @Inject
    internal var toolbar: MainToolbar? = null

    @JvmField
    @Inject
    internal var navigation: MainNavigation? = null

    @JvmField
    @Inject
    internal var container: MainContainer? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by factory<MainViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Fridge_Normal)
        super.onCreate(savedInstanceState)
        val binding = LayoutConstraintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rootView = binding.layoutConstraint
        Injector.obtain<FridgeComponent>(applicationContext)
            .plusMainComponent()
            .create(this, binding.layoutConstraint, guaranteePage(intent), this)
            .inject(this)

        binding.layoutConstraint.makeWindowSexy()
        inflateComponents(binding.layoutConstraint, savedInstanceState)
        beginWork()
    }

    @CheckResult
    private fun guaranteePage(intent: Intent): MainPage {
        return getPage(intent) ?: MainPage.NEED
    }

    @CheckResult
    private fun getPage(intent: Intent): MainPage? {
        return when (getPresenceFromIntent(intent)) {
            FridgeItem.Presence.HAVE -> MainPage.HAVE
            FridgeItem.Presence.NEED -> MainPage.NEED
            else -> null
        }
    }

    override fun checkVersionForUpdate() {
        viewModel.checkForUpdates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val page = getPage(intent)
        if (page != null) {
            viewModel.selectPage(page)
        }
    }

    override fun onStart() {
        super.onStart()
        checkNearbyFragmentPermissions()
    }

    private fun beginWork() {
        this.lifecycleScope.launch(context = Dispatchers.Default) {
            val presence = getPresenceFromIntent(intent)
            requireNotNull(butler).initOnAppStart(
                ButlerParameters(
                    forceNotifyNeeded = presence != FridgeItem.Presence.NEED,
                    forceNotifyExpiring = presence != FridgeItem.Presence.HAVE
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        clearLaunchNotification()
    }

    private fun clearLaunchNotification() {
        val id = intent.getIntExtra(NotificationHandler.NOTIFICATION_ID_KEY, 0)
        if (id != 0) {
            Notifications.cancel(this, id)
        }
    }

    private fun inflateComponents(
        constraintLayout: ConstraintLayout,
        savedInstanceState: Bundle?
    ) {
        val container = requireNotNull(container)
        val toolbar = requireNotNull(toolbar)
        val navigation = requireNotNull(navigation)
        stateSaver = createComponent(
            savedInstanceState, this, viewModel,
            container,
            toolbar,
            navigation
        ) {
            return@createComponent when (it) {
                is MainControllerEvent.PushHave -> pushHave(it.previousPage)
                is MainControllerEvent.PushNeed -> pushNeed(it.previousPage)
                is MainControllerEvent.PushCategory -> pushCategory(it.previousPage)
                is MainControllerEvent.PushNearby -> pushNearby(it.previousPage)
                is MainControllerEvent.NavigateToSettings -> showSettingsDialog()
                is MainControllerEvent.VersionCheck -> checkForUpdate()
            }
        }

        constraintLayout.layout {

            toolbar.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            navigation.also {
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            container.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.BOTTOM, navigation.id(), ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }
        }
    }

    private fun pushHave(previousPage: MainPage?) {
        pushPresenceFragment(previousPage, FridgeItem.Presence.HAVE)
    }

    private fun pushNeed(previousPage: MainPage?) {
        pushPresenceFragment(previousPage, FridgeItem.Presence.NEED)
    }

    private fun pushCategory(previousPage: MainPage?) {
        val fm = supportFragmentManager

        Timber.d("Pushing category fragment")
        fm.commitNow(this) {
            decideAnimationForPage(previousPage, MainPage.CATEGORY)
            replace(fragmentContainerId, CategoryFragment.newInstance(), CategoryFragment.TAG)
        }
    }

    private fun pushNearby(previousPage: MainPage?) {
        val fm = supportFragmentManager
        if (
            fm.findFragmentByTag(MapFragment.TAG) == null &&
            fm.findFragmentByTag(PermissionFragment.TAG) == null
        ) {
            commitNearbyFragment(fm, previousPage)
        }
    }

    private fun checkNearbyFragmentPermissions() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(PermissionFragment.TAG) != null) {
            if (viewModel.canShowMap()) {
                // Replace permission with map
                // Don't need animation because we are already on this page
                commitNearbyFragment(fm, null)
            }
        } else if (fm.findFragmentByTag(MapFragment.TAG) != null) {
            if (!viewModel.canShowMap()) {
                // Replace map with permission
                // Don't need animation because we are already on this page
                commitNearbyFragment(fm, null)
            }
        }
    }

    private fun commitNearbyFragment(fragmentManager: FragmentManager, previousPage: MainPage?) {
        fragmentManager.commitNow(this) {
            val container = fragmentContainerId
            decideAnimationForPage(previousPage, MainPage.NEARBY)
            if (viewModel.canShowMap()) {
                replace(container, MapFragment.newInstance(), MapFragment.TAG)
            } else {
                replace(
                    container,
                    PermissionFragment.newInstance(container),
                    PermissionFragment.TAG
                )
            }
        }
    }

    private fun showSettingsDialog() {
        SettingsDialog().show(this, SettingsDialog.TAG)
    }

    private fun pushPresenceFragment(previousPage: MainPage?, presence: FridgeItem.Presence) {
        val fm = supportFragmentManager

        Timber.d("Pushing fragment: $presence")
        val tag = "${EntryFragment.TAG}|${presence.name}"
        fm.commitNow(this) {
            val newPage = if (presence == FridgeItem.Presence.HAVE) MainPage.HAVE else MainPage.NEED
            decideAnimationForPage(previousPage, newPage)
            replace(fragmentContainerId, EntryFragment.newInstance(presence), tag)
        }
    }

    private fun FragmentTransaction.decideAnimationForPage(oldPage: MainPage?, newPage: MainPage) {
        val (enter, exit) = when (newPage) {
            MainPage.NEED -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.HAVE, MainPage.CATEGORY, MainPage.NEARBY -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.NEED -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.HAVE -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.NEED -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.CATEGORY, MainPage.NEARBY -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.HAVE -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.CATEGORY -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.NEED, MainPage.HAVE -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.NEARBY -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.CATEGORY -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.NEARBY -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.NEED, MainPage.HAVE, MainPage.CATEGORY -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.NEARBY -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
        }
        setCustomAnimations(enter, exit, enter, exit)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DeviceGps.ENABLE_GPS_REQUEST_CODE) {
            handleGpsRequest(resultCode)
        }
    }

    private fun handleGpsRequest(resultCode: Int) {
        viewModel.publishGpsChange(resultCode == Activity.RESULT_OK)
    }

    override fun onDestroy() {
        super.onDestroy()
        rootView = null
        stateSaver = null

        toolbar = null
        container = null
        navigation = null

        factory = null
    }

    companion object {

        @JvmStatic
        @CheckResult
        private fun getPresenceFromIntent(intent: Intent): FridgeItem.Presence? {
            val presenceString: String? = intent.getStringExtra(FridgeItem.Presence.KEY)
            return if (presenceString == null) null else FridgeItem.Presence.valueOf(presenceString)
        }
    }
}
