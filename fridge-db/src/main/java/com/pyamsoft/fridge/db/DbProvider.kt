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

package com.pyamsoft.fridge.db

import android.content.Context
import androidx.annotation.CheckResult
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import java.util.Date
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Singleton

@Module
object DbProvider {

  @CheckResult
  @JvmStatic
  private fun createDateAwareMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder().add(Date::class.java, Rfc3339DateJsonAdapter()).build()
  }

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideFridgeItemListRepo(
    context: Context,
    moshi: Moshi
  ): Repo<List<JsonMappableFridgeItem>> {
//    val type = Types.newParameterizedType(List::class.java, JsonMappableFridgeItem::class.java)
    return newRepoBuilder<List<JsonMappableFridgeItem>>()
      .memoryCache(5, MINUTES)
//      .persister(
//        2, HOURS, File(context.cacheDir, "fridge-item-repo"),
//        MoshiPersister.create(createDateAwareMoshi(moshi), type)
//      )
      .build()
  }

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideFridgeEntryListRepo(
    context: Context,
    moshi: Moshi
  ): Repo<List<JsonMappableFridgeEntry>> {
//    val type = Types.newParameterizedType(List::class.java, JsonMappableFridgeEntry::class.java)
    return newRepoBuilder<List<JsonMappableFridgeEntry>>()
      .memoryCache(5, MINUTES)
//      .persister(
//        2, HOURS, File(context.cacheDir, "fridge-entry-repo"),
//        MoshiPersister.create(createDateAwareMoshi(moshi), type)
//      )
      .build()
  }

}