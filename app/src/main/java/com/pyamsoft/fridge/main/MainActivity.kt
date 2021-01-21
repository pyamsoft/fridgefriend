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
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.pyamsoft.fridge.BuildConfig
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.category.CategoryFragment
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.entry.EntryFragment
import com.pyamsoft.fridge.initOnAppStart
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.fridge.permission.PermissionFragment
import com.pyamsoft.fridge.search.SearchFragment
import com.pyamsoft.fridge.setting.SettingsFragment
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.fridge.ui.appbar.AppBarActivityProvider
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.createSavedStateViewModelFactory
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.changelog.ChangeLogActivity
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.doOnResume
import com.pyamsoft.pydroid.util.doOnStart
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.fragment.R as R2

internal class MainActivity : ChangeLogActivity(),
    VersionChecker,
    AppBarActivity,
    AppBarActivityProvider {

    private var isUpdateChecked = false

    override val checkForUpdates = false

    override val applicationIcon = R.mipmap.ic_launcher

    override val versionName = BuildConfig.VERSION_NAME

    override val changelog = buildChangeLog {
        change("The NEED and HAVE tabs in a group view have moved to the top toolbar")
        feature("Group names can be edited via long press")
//        // TODO(Do this)
//        feature("More information on Group overview screen")
//        // TODO(Do this): Entry screen empty state
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
                    Timber.d("Return fragment snackbar container: $fragment $container")
                    return container
                }
            }

            val fallbackContainer = requireNotNull(snackbar?.container())
            Timber.d("Return activity snackbar container: $fallbackContainer")
            return fallbackContainer
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
    internal var factory: MainViewModel.Factory? = null
    private val viewModel by fromViewModelFactory<MainViewModel> {
        createSavedStateViewModelFactory(factory)
    }

    private val handler = Handler(Looper.getMainLooper())

    private var capturedAppBar: AppBarLayout? = null

    override fun setAppBar(bar: AppBarLayout?) {
        capturedAppBar = bar
    }

    override fun requireAppBar(func: (AppBarLayout) -> Unit) {
        requireNotNull(capturedAppBar).let(func)
    }

    override fun withAppBar(func: (AppBarLayout) -> Unit) {
        capturedAppBar?.let(func)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Fridge)
        super.onCreate(savedInstanceState)
        val binding = LayoutConstraintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Injector.obtainFromApplication<FridgeComponent>(this)
            .plusMainComponent()
            .create(this, this, this, binding.layoutConstraint, this, this)
            .inject(this)

        stableLayoutHideNavigation()

        inflateComponents(binding.layoutConstraint, savedInstanceState)
        beginWork()

        handleIntentExtras(intent)
    }

    override fun onVersionCheck() {
        if (!isUpdateChecked) {
            isUpdateChecked = true
            Timber.d("Queue new update check")

            // Queue this for doOnResume because the VersionCheck code is only available after
            // onPostCreate and will otherwise throw an NPE
            doOnResume {
                Timber.d("Checking for a new update")
                checkForUpdates()
            }
        }
    }

    private inline fun handleNotificationAction(crossinline action: (FragmentManager) -> Unit) {
        handler.removeCallbacksAndMessages(null)

        // No good way to figure out when the FM is done transacting from a different context I think
        //
        // Assuming that the FM handler uses the main thread, we post twice
        // The first post puts us into the queue and basically waits for everything to clear out
        // this would include the FM pending transactions which may also include the page select
        // commit.
        //
        // We post delayed just in case the transaction takes longer than it takes to queue the new message up
        // to be safe, we should try to use commitNow whenever possible.
        handler.post {
            handler.postDelayed({
                val fm = supportFragmentManager

                // Clear the back stack
                // In case we are already on a detail fragment for example, this will clear the stack.
                fm.clearBackStack()
                action(fm)
            }, 200)
        }
    }

    @CheckResult
    private fun handleEntryIntent(intent: Intent): Boolean {
        val stringEntryId = intent.getStringExtra(NotificationHandler.KEY_ENTRY_ID) ?: return false

        val pres = intent.getStringExtra(NotificationHandler.KEY_PRESENCE_TYPE)
        if (pres == null) {
            Timber.d("New intent had entry key but no presence type")
            return false
        }

        val entryId = FridgeEntry.Id(stringEntryId)
        val presence = FridgeItem.Presence.valueOf(pres)
        Timber.d("Entries page selected, load entry $entryId with presence: $presence")

        viewModel.selectPage(force = true, MainPage.Entries)
        handleNotificationAction { fm ->
            EntryFragment.pushDetailPage(
                fm,
                this,
                fragmentContainerId,
                entryId,
                presence
            )
        }
        return true
    }

    @CheckResult
    private fun handleNearbyIntent(intent: Intent): Boolean {
        val longNearbyId = intent.getLongExtra(NotificationHandler.KEY_NEARBY_ID, 0L)
        if (longNearbyId == 0L) {
            return false
        }

        val nearbyType = intent.getStringExtra(NotificationHandler.KEY_NEARBY_TYPE)
        if (nearbyType == null) {
            Timber.d("New intent had nearby key but no type")
            return false
        }
        val nearbyStoreId: NearbyStore.Id
        val nearbyZoneId: NearbyZone.Id
        when (nearbyType) {
            NotificationHandler.VALUE_NEARBY_TYPE_STORE -> {
                nearbyStoreId = NearbyStore.Id(longNearbyId)
                nearbyZoneId = NearbyZone.Id.EMPTY
            }
            NotificationHandler.VALUE_NEARBY_TYPE_ZONE -> {
                nearbyStoreId = NearbyStore.Id.EMPTY
                nearbyZoneId = NearbyZone.Id(longNearbyId)
            }
            else -> return false
        }

        Timber.d("Map page selected, load nearby: $nearbyStoreId $nearbyZoneId")
        viewModel.selectPage(
            force = true, MainPage.Nearby(
                storeId = nearbyStoreId,
                zoneId = nearbyZoneId
            )
        )
        return true
    }

    private fun handleIntentExtras(intent: Intent) {
        if (handleEntryIntent(intent)) {
            Timber.d("New intent handled entry extras")
            return
        }

        if (handleNearbyIntent(intent)) {
            Timber.d("New intent handled nearby extras")
            return
        }

        Timber.d("New intent no extras")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
    }

    override fun onBackPressed() {
        onBackPressedDispatcher.also { dispatcher ->
            if (dispatcher.hasEnabledCallbacks()) {
                dispatcher.onBackPressed()
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkNearbyFragmentPermissions()
    }

    override fun onResume() {
        super.onResume()
        clearLaunchNotification()
    }

    private fun beginWork() {
        this.lifecycleScope.launch(context = Dispatchers.Default) {
            initOnAppStart(requireNotNull(butler), requireNotNull(orderFactory))
        }
    }

    private fun clearLaunchNotification() {
        val id = intent.getIntExtra(NotificationHandler.KEY_NOTIFICATION_ID, 0)
        if (id != 0) {
            requireNotNull(notificationHandler).cancel(id.toNotifyId())
        }
    }

    private fun inflateComponents(
        constraintLayout: ConstraintLayout,
        savedInstanceState: Bundle?,
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
                is MainControllerEvent.PushEntry -> pushEntry(it.previousPage, it.force)
                is MainControllerEvent.PushCategory -> pushCategory(it.previousPage, it.force)
                is MainControllerEvent.PushSettings -> pushSettings(it.previousPage, it.force)
                is MainControllerEvent.PushSearch -> pushSearch(it.previousPage, it.force)
                is MainControllerEvent.PushNearby -> pushNearby(
                    it.previousPage,
                    it.storeId,
                    it.zoneId,
                    it.force
                )
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

        val existingFragment = supportFragmentManager.findFragmentById(fragmentContainerId)
        if (savedInstanceState == null || existingFragment == null) {
            viewModel.loadDefaultPage()
        }
    }

    private fun pushSearch(previousPage: MainPage?, force: Boolean) {
        commitPage(
            SearchFragment.newInstance(),
            MainPage.Search,
            previousPage,
            SearchFragment.TAG,
            force
        )
    }

    private fun pushSettings(previousPage: MainPage?, force: Boolean) {
        commitPage(
            SettingsFragment.newInstance(),
            MainPage.Settings,
            previousPage,
            SettingsFragment.TAG,
            force
        )
    }

    private fun pushCategory(previousPage: MainPage?, force: Boolean) {
        commitPage(
            CategoryFragment.newInstance(),
            MainPage.Category,
            previousPage,
            CategoryFragment.TAG,
            force
        )
    }

    private fun pushNearby(
        previousPage: MainPage?,
        storeId: NearbyStore.Id,
        zoneId: NearbyZone.Id,
        force: Boolean,
    ) {
        commitNearbyFragment(previousPage, storeId, zoneId, force)
    }

    private fun checkNearbyFragmentPermissions() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(PermissionFragment.TAG) != null) {
            viewModel.withForegroundPermission(withPermission = {
                // Replace permission with map
                // Don't need animation because we are already on this page
                Timber.d("Permission gained, commit Map fragment")
                commitMapFragment(
                    null,
                    storeId = NearbyStore.Id.EMPTY,
                    zoneId = NearbyZone.Id.EMPTY,
                    force = true
                )
            })
        } else if (fm.findFragmentByTag(MapFragment.TAG) != null) {
            viewModel.withForegroundPermission(withoutPermission = {
                // Replace map with permission
                // Don't need animation because we are already on this page
                Timber.d("Permission lost, commit Permission fragment")
                commitPermissionFragment(null, force = true)
            })
        }
    }

    private fun commitMapFragment(
        previousPage: MainPage?,
        storeId: NearbyStore.Id,
        zoneId: NearbyZone.Id, force: Boolean,
    ) {
        commitPage(
            MapFragment.newInstance(storeId, zoneId),
            MainPage.Nearby(storeId, zoneId),
            previousPage,
            MapFragment.TAG,
            force
        )
    }

    private fun commitPermissionFragment(previousPage: MainPage?, force: Boolean) {
        commitPage(
            PermissionFragment.newInstance(fragmentContainerId),
            MainPage.Nearby(storeId = NearbyStore.Id.EMPTY, zoneId = NearbyZone.Id.EMPTY),
            previousPage,
            PermissionFragment.TAG,
            force
        )
    }

    private fun commitNearbyFragment(
        previousPage: MainPage?,
        storeId: NearbyStore.Id,
        zoneId: NearbyZone.Id,
        force: Boolean,
    ) {
        viewModel.withForegroundPermission(
            withPermission = { commitMapFragment(previousPage, storeId, zoneId, force) },
            withoutPermission = { commitPermissionFragment(previousPage, force) }
        )
    }

    private fun pushEntry(previousPage: MainPage?, force: Boolean) {
        commitPage(
            EntryFragment.newInstance(fragmentContainerId),
            MainPage.Entries,
            previousPage,
            EntryFragment.TAG,
            force
        )
    }

    private fun commitPage(
        fragment: Fragment,
        newPage: MainPage,
        previousPage: MainPage?,
        tag: String,
        force: Boolean,
    ) {
        val fm = supportFragmentManager
        val container = fragmentContainerId

        val push = when {
            previousPage != null -> true
            fm.findFragmentById(container) == null -> true
            else -> false
        }

        if (push || force) {
            if (force) {
                Timber.d("Force commit fragment: $tag")
            } else {
                Timber.d("Commit fragment: $tag")
            }

            this.doOnStart {
                // Clear the back stack (for entry->detail stack)
                fm.clearBackStack()

                fm.commitNow(this) {
                    decideAnimationForPage(previousPage, newPage)
                    replace(container, fragment, tag)
                }
            }
        }
    }

    private fun FragmentTransaction.decideAnimationForPage(oldPage: MainPage?, newPage: MainPage) {
        val animations = when (newPage) {
            is MainPage.Search -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                is MainPage.Entries -> R.anim.slide_in_right to R.anim.slide_out_left
                is MainPage.Category, is MainPage.Nearby, is MainPage.Settings -> R.anim.slide_in_left to R.anim.slide_out_right
                is MainPage.Search -> null
            }
            is MainPage.Entries -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                is MainPage.Search, is MainPage.Category, is MainPage.Nearby, is MainPage.Settings -> R.anim.slide_in_left to R.anim.slide_out_right
                is MainPage.Entries -> null
            }
            is MainPage.Category -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                is MainPage.Search, is MainPage.Entries -> R.anim.slide_in_right to R.anim.slide_out_left
                is MainPage.Nearby, is MainPage.Settings -> R.anim.slide_in_left to R.anim.slide_out_right
                is MainPage.Category -> null
            }
            is MainPage.Nearby -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                is MainPage.Search, is MainPage.Entries, is MainPage.Category -> R.anim.slide_in_right to R.anim.slide_out_left
                is MainPage.Settings -> R.anim.slide_in_left to R.anim.slide_out_right
                is MainPage.Nearby -> null
            }
            is MainPage.Settings -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                is MainPage.Search, is MainPage.Entries, is MainPage.Category, is MainPage.Nearby -> R.anim.slide_in_right to R.anim.slide_out_left
                is MainPage.Settings -> null
            }
        }

        if (animations != null) {
            val (enter, exit) = animations
            setCustomAnimations(enter, exit, enter, exit)
        }
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

        capturedAppBar = null

        handler.removeCallbacksAndMessages(null)
    }

    private fun FragmentManager.clearBackStack() {
        Timber.d("Clear FM back stack")
        this.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}
