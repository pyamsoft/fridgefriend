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

package com.pyamsoft.fridge.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.fridge.BuildConfig
import com.pyamsoft.fridge.ButlerParameters
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.order.OrderFactory
import com.pyamsoft.fridge.category.CategoryFragment
import com.pyamsoft.fridge.entry.EntryFragment
import com.pyamsoft.fridge.initOnAppStart
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.fridge.permission.PermissionFragment
import com.pyamsoft.fridge.setting.SettingsFragment
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.rating.ChangeLogBuilder
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import com.pyamsoft.pydroid.ui.rating.buildChangeLog
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class MainActivity : RatingActivity(), VersionChecker {

    override val checkForUpdates: Boolean = false

    override val applicationIcon: Int = R.mipmap.ic_launcher

    override val versionName: String = BuildConfig.VERSION_NAME

    override val changeLogLines: ChangeLogBuilder = buildChangeLog {
        change("Faster database performance")
        change("Better UI responsiveness")
        bugfix("Fix Map page sometimes rendering incorrectly")
        change("Schedule notifications more efficiently to save battery")
    }

    override val fragmentContainerId: Int
        get() = requireNotNull(container).id()

    override val snackbarRoot: ViewGroup
        get() {
            val fm = supportFragmentManager
            val fragment = fm.findFragmentById(fragmentContainerId)
            if (fragment is SnackbarContainer) {
                val container = fragment.container()
                if (container != null) {
                    return container
                }
            }

            return requireNotNull(snackbar?.container())
        }

    private var stateSaver: StateSaver? = null

    @JvmField
    @Inject
    internal var butler: Butler? = null

    @JvmField
    @Inject
    internal var orderFactory: OrderFactory? = null

    @JvmField
    @Inject
    internal var notificationHandler: NotificationHandler? = null

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
    internal var snackbar: MainSnackbar? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by viewModelFactory<MainViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Fridge_Normal)
        super.onCreate(savedInstanceState)
        val binding = LayoutConstraintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Injector.obtain<FridgeComponent>(applicationContext)
            .plusMainComponent()
            .create(this, binding.layoutConstraint, guaranteePage(intent), this)
            .inject(this)

        stableLayoutHideNavigation()

        inflateComponents(binding.layoutConstraint, savedInstanceState)
        beginWork()
    }

    @CheckResult
    private fun guaranteePage(intent: Intent): MainPage {
        // TODO(Peter): We must open the right page
        // Additionally pass a FridgeItem.Presence to open the correct tab within the selected entry
        return MainPage.ENTRIES
    }

    override fun checkVersionForUpdate() {
        viewModel.checkForUpdates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

//        val page = getPage(intent)
//        if (page != null) {
//            viewModel.selectPage(page)
//        }
    }

    override fun onStart() {
        super.onStart()
        checkNearbyFragmentPermissions()
    }

    private fun beginWork() {
        this.lifecycleScope.launch(context = Dispatchers.Default) {
            requireNotNull(butler).initOnAppStart(
                requireNotNull(orderFactory),
                ButlerParameters(forceNotifyNeeded = false, forceNotifyExpiring = false)
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
            requireNotNull(notificationHandler).cancel(id.toNotifyId())
        }
    }

    private fun inflateComponents(
        constraintLayout: ConstraintLayout,
        savedInstanceState: Bundle?
    ) {
        val container = requireNotNull(container)
        val toolbar = requireNotNull(toolbar)
        val navigation = requireNotNull(navigation)
        val snackbar = requireNotNull(snackbar)
        stateSaver = createComponent(
            savedInstanceState,
            this,
            viewModel,
            container,
            toolbar,
            navigation,
            snackbar
        ) {
            return@createComponent when (it) {
                is MainControllerEvent.PushEntry -> pushEntry(it.previousPage)
                is MainControllerEvent.PushCategory -> pushCategory(it.previousPage)
                is MainControllerEvent.PushNearby -> pushNearby(it.previousPage)
                is MainControllerEvent.PushSettings -> pushSettings(it.previousPage)
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
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            snackbar.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.BOTTOM, navigation.id(), ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }
        }
    }

    private fun pushSettings(previousPage: MainPage?) {
        commitPage(
            SettingsFragment.newInstance(),
            MainPage.SETTINGS,
            previousPage,
            SettingsFragment.TAG
        )
    }

    private fun pushCategory(previousPage: MainPage?) {
        commitPage(
            CategoryFragment.newInstance(),
            MainPage.CATEGORY,
            previousPage,
            CategoryFragment.TAG
        )
    }

    private fun pushNearby(previousPage: MainPage?) {
        val fm = supportFragmentManager
        if (
            fm.findFragmentByTag(MapFragment.TAG) == null &&
            fm.findFragmentByTag(PermissionFragment.TAG) == null
        ) {
            commitNearbyFragment(previousPage)
        }
    }

    private fun checkNearbyFragmentPermissions() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(PermissionFragment.TAG) != null) {
            viewModel.withForegroundPermission(withPermission = {
                // Replace permission with map
                // Don't need animation because we are already on this page
                Timber.d("Permission gained, commit Map fragment")
                commitMapFragment(null, forcePush = true)
            })
        } else if (fm.findFragmentByTag(MapFragment.TAG) != null) {
            viewModel.withForegroundPermission(withoutPermission = {
                // Replace map with permission
                // Don't need animation because we are already on this page
                Timber.d("Permission lost, commit Permission fragment")
                commitPermissionFragment(null, forcePush = true)
            })
        }
    }

    private fun commitMapFragment(previousPage: MainPage?, forcePush: Boolean = false) {
        commitPage(
            MapFragment.newInstance(),
            MainPage.NEARBY,
            previousPage,
            MapFragment.TAG,
            forcePush
        )
    }

    private fun commitPermissionFragment(previousPage: MainPage?, forcePush: Boolean = false) {
        commitPage(
            PermissionFragment.newInstance(fragmentContainerId),
            MainPage.NEARBY,
            previousPage,
            PermissionFragment.TAG,
            forcePush
        )
    }

    private fun commitNearbyFragment(previousPage: MainPage?) {
        viewModel.withForegroundPermission(
            withPermission = { commitMapFragment(previousPage) },
            withoutPermission = { commitPermissionFragment(previousPage) }
        )
    }

    private fun pushEntry(previousPage: MainPage?) {
        commitPage(EntryFragment.newInstance(), MainPage.ENTRIES, previousPage, EntryFragment.TAG)
    }

    private fun commitPage(
        fragment: Fragment,
        newPage: MainPage,
        previousPage: MainPage?,
        tag: String,
        forcePush: Boolean = false
    ) {
        val fm = supportFragmentManager
        val container = fragmentContainerId

        val push = when {
            previousPage != null -> true
            fm.findFragmentById(container) == null -> true
            else -> false
        }

        if (push || forcePush) {
            if (forcePush) {
                Timber.d("Force commit fragment: $tag")
            } else {
                Timber.d("Commit fragment: $tag")
            }
            fm.commitNow(this) {
                decideAnimationForPage(previousPage, newPage)
                replace(container, fragment, tag)
            }
        }
    }

    private fun FragmentTransaction.decideAnimationForPage(oldPage: MainPage?, newPage: MainPage) {
        val (enter, exit) = when (newPage) {
            MainPage.ENTRIES -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.CATEGORY, MainPage.NEARBY, MainPage.SETTINGS -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.ENTRIES -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.CATEGORY -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.ENTRIES -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.NEARBY, MainPage.SETTINGS -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.CATEGORY -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.NEARBY -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.ENTRIES, MainPage.CATEGORY -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.SETTINGS -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.NEARBY -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.SETTINGS -> when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                MainPage.ENTRIES, MainPage.CATEGORY, MainPage.NEARBY -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.SETTINGS -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
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
        stateSaver = null

        toolbar = null
        container = null
        navigation = null
        snackbar = null

        factory = null
    }
}
