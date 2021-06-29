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

package com.pyamsoft.fridge.main

import android.content.Intent
import android.graphics.Color
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
import com.pyamsoft.fridge.entry.EntryFragment
import com.pyamsoft.fridge.initOnAppStart
import com.pyamsoft.fridge.search.SearchFragment
import com.pyamsoft.fridge.setting.SettingsFragment
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.ui.app.AppBarActivity
import com.pyamsoft.pydroid.ui.app.AppBarActivityProvider
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiController
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.createSavedStateViewModelFactory
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.changelog.ChangeLogActivity
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.doOnResume
import com.pyamsoft.pydroid.util.doOnStart
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class MainActivity :
    ChangeLogActivity(),
    VersionChecker,
    AppBarActivity,
    AppBarActivityProvider,
    UiController<MainControllerEvent> {

  private var isUpdateChecked = false

  override val checkForUpdates = false

  override val applicationIcon = R.mipmap.ic_launcher

  override val versionName = BuildConfig.VERSION_NAME

  override val changelog = buildChangeLog {
    feature(
        "Adds the ability to quickly re-add an Item to your Shopping list when swiping it away for Consume or Spoil")
    bugfix("Preserve the search state on device rotation on the Search screen")
    //        // TODO(Do this)
    //        feature("More information on Group overview screen")
    //        // TODO(Do this): Entry screen empty state
    // TODO(Do this)
    // When selecting suggested items, we can try to derive a new expiration date
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

  @JvmField @Inject internal var butler: Butler? = null

  @JvmField @Inject internal var orderFactory: OrderFactory? = null

  @JvmField @Inject internal var notificationHandler: NotificationHandler? = null

  @JvmField @Inject internal var toolbar: MainToolbar? = null

  @JvmField @Inject internal var navigation: MainNavigation? = null

  @JvmField @Inject internal var container: MainContainer? = null

  @JvmField @Inject internal var snackbar: MainSnackbar? = null

  @JvmField @Inject internal var factory: MainViewModel.Factory? = null
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
    val binding = LayoutCoordinatorBinding.inflate(layoutInflater)
    setContentView(binding.root)

    Injector.obtainFromApplication<FridgeComponent>(this)
        .plusMainComponent()
        .create(this, this, this, binding.layoutCoordinator, this, this)
        .inject(this)

    stableLayoutHideNavigation()

    inflateComponents(savedInstanceState)
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
    // We post delayed just in case the transaction takes longer than it takes to queue the new
    // message up
    // to be safe, we should try to use commitNow whenever possible.
    handler.post {
      handler.postDelayed(
          {
            val fm = supportFragmentManager

            // Clear the back stack
            // In case we are already on a detail fragment for example, this will clear the stack.
            fm.clearBackStack()
            action(fm)
          },
          200)
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

    viewModel.handleSelectPage(MainPage.Entries, force = true)

    handleNotificationAction { fm ->
      EntryFragment.pushDetailPage(fm, this, fragmentContainerId, entryId, presence)
    }
    return true
  }

  private fun handleIntentExtras(intent: Intent) {
    if (handleEntryIntent(intent)) {
      Timber.d("New intent handled entry extras")
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

  override fun onResume() {
    super.onResume()
    clearLaunchNotification()
  }

  private fun beginWork() {
    this.lifecycleScope.launch(context = Dispatchers.Default) {
      requireNotNull(butler).initOnAppStart(requireNotNull(orderFactory))
    }
  }

  private fun clearLaunchNotification() {
    val id = intent.getIntExtra(NotificationHandler.KEY_NOTIFICATION_ID, 0)
    if (id != 0) {
      requireNotNull(notificationHandler).cancel(id.toNotifyId())
    }
  }

  private fun handleSelectPage(newPage: MainPage, oldPage: MainPage?, force: Boolean) {
    return when (newPage) {
      is MainPage.Entries -> pushEntry(oldPage, force)
      is MainPage.Search -> pushSearch(oldPage, force)
      is MainPage.Settings -> pushSettings(oldPage, force)
      is MainPage.Category -> pushCategory(oldPage, force)
    }
  }

  private fun inflateComponents(
      savedInstanceState: Bundle?,
  ) {
    val container = requireNotNull(container)
    val toolbar = requireNotNull(toolbar)
    val navigation = requireNotNull(navigation)
    val snackbar = requireNotNull(snackbar)

    stateSaver =
        createComponent(
            savedInstanceState, this, viewModel, this, container, toolbar, navigation, snackbar) {
          return@createComponent when (it) {
            is MainViewEvent.BottomBarMeasured -> viewModel.handleConsumeBottomBarHeight(it.height)
            is MainViewEvent.OpenCategory ->
                viewModel.handleSelectPage(MainPage.Category, force = false)
            is MainViewEvent.OpenEntries ->
                viewModel.handleSelectPage(
                    MainPage.Entries,
                    force = false,
                )
            is MainViewEvent.OpenSearch ->
                viewModel.handleSelectPage(MainPage.Search, force = false)
            is MainViewEvent.OpenSettings ->
                viewModel.handleSelectPage(MainPage.Settings, force = false)
          }
        }

    val existingFragment = supportFragmentManager.findFragmentById(fragmentContainerId)
    if (savedInstanceState == null || existingFragment == null) {
      viewModel.handleLoadDefaultPage()
    }
  }

  override fun onControllerEvent(event: MainControllerEvent) {
    return when (event) {
      is MainControllerEvent.PushPage -> handleSelectPage(event.newPage, event.oldPage, event.force)
    }
  }

  private fun pushSearch(previousPage: MainPage?, force: Boolean) {
    commitPage(
        SearchFragment.newInstance(), MainPage.Search, previousPage, SearchFragment.TAG, force)
  }

  private fun pushSettings(previousPage: MainPage?, force: Boolean) {
    commitPage(
        SettingsFragment.newInstance(),
        MainPage.Settings,
        previousPage,
        SettingsFragment.TAG,
        force)
  }

  private fun pushCategory(previousPage: MainPage?, force: Boolean) {
    commitPage(
        CategoryFragment.newInstance(),
        MainPage.Category,
        previousPage,
        CategoryFragment.TAG,
        force)
  }

  private fun pushEntry(previousPage: MainPage?, force: Boolean) {
    commitPage(
        EntryFragment.newInstance(fragmentContainerId),
        MainPage.Entries,
        previousPage,
        EntryFragment.TAG,
        force)
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

    val push =
        when {
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
    val animations =
        when (newPage) {
          is MainPage.Search ->
              when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                is MainPage.Entries -> R.anim.slide_in_right to R.anim.slide_out_left
                is MainPage.Category, is MainPage.Settings ->
                    R.anim.slide_in_left to R.anim.slide_out_right
                is MainPage.Search -> null
              }
          is MainPage.Entries ->
              when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                is MainPage.Search, is MainPage.Category, is MainPage.Settings ->
                    R.anim.slide_in_left to R.anim.slide_out_right
                is MainPage.Entries -> null
              }
          is MainPage.Category ->
              when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                is MainPage.Search, is MainPage.Entries ->
                    R.anim.slide_in_right to R.anim.slide_out_left
                is MainPage.Settings -> R.anim.slide_in_left to R.anim.slide_out_right
                is MainPage.Category -> null
              }
          is MainPage.Settings ->
              when (oldPage) {
                null -> R.anim.fragment_open_enter to R.anim.fragment_open_exit
                is MainPage.Search, is MainPage.Entries, is MainPage.Category ->
                    R.anim.slide_in_right to R.anim.slide_out_left
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
