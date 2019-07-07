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

package com.pyamsoft.fridge.base

import androidx.annotation.CheckResult
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import com.pyamsoft.pydroid.arch.UiViewModel

/**
 * Allow nullable for easier caller API
 */
@CheckResult
internal inline fun <reified T : UiViewModel<*, *, *>> Fragment.fromFactory(factory: Factory?): T {
  return ViewModelProviders.of(this, requireNotNull(factory))
      .get()
}

@CheckResult
internal inline fun <reified T : UiViewModel<*, *, *>> ViewModelFactoryFragment.fromFactory(): T {
  return this.fromFactory(factory)
}

/**
 * Allow nullable for easier caller API
 */
internal inline fun Fragment.withFactory(
  factory: Factory?,
  func: ViewModelProvider.() -> Unit
) {
  return func(ViewModelProviders.of(this, requireNotNull(factory)))
}

internal inline fun ViewModelFactoryFragment.withFactory(func: ViewModelProvider.() -> Unit) {
  return this.withFactory(factory, func)
}

/**
 * Allow nullable for easier caller API
 */
@CheckResult
internal inline fun <reified T : UiViewModel<*, *, *>> FragmentActivity.fromFactory(factory: Factory?): T {
  return ViewModelProviders.of(this, requireNotNull(factory))
      .get()
}

@CheckResult
internal inline fun <reified T : UiViewModel<*, *, *>> ViewModelFactoryActivity.fromFactory(): T {
  return this.fromFactory(factory)
}

/**
 * Allow nullable for easier caller API
 */
internal inline fun FragmentActivity.withFactory(
  factory: Factory?,
  func: ViewModelProvider.() -> Unit
) {
  return func(ViewModelProviders.of(this, requireNotNull(factory)))
}

internal inline fun ViewModelFactoryActivity.withFactory(func: ViewModelProvider.() -> Unit) {
  return this.withFactory(factory, func)
}

/**
 * Allow nullable for easier caller API
 */
@CheckResult
internal inline fun <reified T : UiViewModel<*, *, *>> DialogFragment.fromFactory(factory: Factory?): T {
  return ViewModelProviders.of(this, requireNotNull(factory))
      .get()
}

@CheckResult
internal inline fun <reified T : UiViewModel<*, *, *>> ViewModelFactoryDialog.fromFactory(): T {
  return this.fromFactory(factory)
}

/**
 * Allow nullable for easier caller API
 */
internal inline fun DialogFragment.withFactory(
  factory: Factory?,
  func: ViewModelProvider.() -> Unit
) {
  return func(ViewModelProviders.of(this, requireNotNull(factory)))
}

internal inline fun ViewModelFactoryDialog.withFactory(func: ViewModelProvider.() -> Unit) {
  return this.withFactory(factory, func)
}
