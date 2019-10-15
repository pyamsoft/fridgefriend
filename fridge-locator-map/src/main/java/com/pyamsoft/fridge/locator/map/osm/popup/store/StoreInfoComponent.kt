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

package com.pyamsoft.fridge.locator.map.osm.popup.store

import android.location.Location
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.store.NearbyStoreRealtime
import com.pyamsoft.fridge.locator.map.osm.popup.PopupInfoScope
import com.pyamsoft.fridge.locator.map.osm.popup.PopupViewModelFactory
import com.pyamsoft.fridge.locator.map.osm.popup.ViewModelKey
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoComponent.ViewModelModule
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.loader.ImageLoader
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.multibindings.IntoMap

@PopupInfoScope
@Component(modules = [ViewModelModule::class])
internal interface StoreInfoComponent {

    fun inject(infoWindow: StoreInfoWindow)

    @Component.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance myLocation: Location?,
            @BindsInstance parent: ViewGroup,
            @BindsInstance imageLoader: ImageLoader,
            @BindsInstance store: NearbyStore,
            @BindsInstance butler: Butler,
            @BindsInstance nearbyStoreRealtime: NearbyStoreRealtime,
            @BindsInstance nearbyStoreQueryDao: NearbyStoreQueryDao,
            @BindsInstance nearbyStoreInsertDao: NearbyStoreInsertDao,
            @BindsInstance nearbyStoreDeleteDao: NearbyStoreDeleteDao
        ): StoreInfoComponent
    }

    @Module
    abstract class ViewModelModule {

        @Binds
        internal abstract fun bindViewModelFactory(factory: PopupViewModelFactory): ViewModelProvider.Factory

        @Binds
        @IntoMap
        @ViewModelKey(StoreInfoViewModel::class)
        internal abstract fun storeViewModel(viewModel: StoreInfoViewModel): UiViewModel<*, *, *>
    }
}
