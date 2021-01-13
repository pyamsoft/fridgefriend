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

package com.pyamsoft.fridge.core

import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelFactory
import com.pyamsoft.pydroid.arch.UiStateViewModel
import com.pyamsoft.pydroid.arch.UiViewModelFactory
import javax.inject.Provider
import kotlin.reflect.KClass

@CheckResult
@JvmOverloads
inline fun <reified T : ViewModel> SavedStateRegistryOwner.createAssistedFactory(
    factory: AssistedFridgeViewModelFactory<T>?,
    defaultArgs: Bundle? = null
): UiSavedStateViewModelFactory {
    return object : UiSavedStateViewModelFactory(this, defaultArgs) {
        override fun <T : UiStateViewModel<*>> viewModel(
            modelClass: KClass<T>,
            savedState: UiSavedState
        ): UiStateViewModel<*> {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(factory).create(savedState) as? T ?: fail()
        }

    }
}

@CheckResult
inline fun <reified T : ViewModel> createFactory(factory: Provider<T>?): UiViewModelFactory {
    return object : UiViewModelFactory() {
        override fun <T : UiStateViewModel<*>> viewModel(modelClass: KClass<T>): UiStateViewModel<*> {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(factory).get() as? T ?: fail()
        }
    }
}

interface AssistedFridgeViewModelFactory<T : ViewModel> {

    fun create(savedState: UiSavedState): T
}

