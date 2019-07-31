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

package com.pyamsoft.fridge.locator.map.osm

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.locator.map.osm.popup.ZoneInfoWindow
import dagger.BindsInstance
import dagger.Component

@Component
@MapViewScope
internal interface MapViewComponent {

  fun inject(infoWindow: ZoneInfoWindow)

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
      @BindsInstance nearbyStoreInsertDao: NearbyStoreInsertDao,
      @BindsInstance nearbyStoreDeleteDao: NearbyStoreDeleteDao,
      @BindsInstance nearbyZoneInsertDao: NearbyZoneInsertDao,
      @BindsInstance nearbyZoneDeleteDao: NearbyZoneDeleteDao
    ): MapViewComponent
  }

}
