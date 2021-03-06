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

package com.pyamsoft.fridge.butler

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.notification.NotificationHandlerImpl
import com.pyamsoft.fridge.butler.notification.dispatcher.ExpiredItemNotifyDispatcher
import com.pyamsoft.fridge.butler.notification.dispatcher.ExpiringItemNotifyDispatcher
import com.pyamsoft.fridge.butler.notification.dispatcher.NeededItemNotifyDispatcher
import com.pyamsoft.fridge.butler.notification.dispatcher.NightlyNotifyDispatcher
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.butler.work.order.OrderFactoryImpl
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Qualifier

@Qualifier @Retention(AnnotationRetention.BINARY) internal annotation class ButlerInternalApi

@Module
abstract class ButlerModule {

  @Binds
  @CheckResult
  internal abstract fun bindNotificationHandler(impl: NotificationHandlerImpl): NotificationHandler

  @Binds
  @IntoSet
  @ButlerInternalApi
  internal abstract fun bindNeededDispatcher(impl: NeededItemNotifyDispatcher): NotifyDispatcher<*>

  @Binds
  @IntoSet
  @ButlerInternalApi
  internal abstract fun bindExpiringDispatcher(
      impl: ExpiringItemNotifyDispatcher
  ): NotifyDispatcher<*>

  @Binds
  @IntoSet
  @ButlerInternalApi
  internal abstract fun bindExpiredDispatcher(
      impl: ExpiredItemNotifyDispatcher
  ): NotifyDispatcher<*>

  @Binds
  @IntoSet
  @ButlerInternalApi
  internal abstract fun bindNightlyDispatcher(impl: NightlyNotifyDispatcher): NotifyDispatcher<*>

  @Binds @CheckResult internal abstract fun bindOrderFactory(impl: OrderFactoryImpl): OrderFactory

  @Module
  companion object {

    @Provides
    @JvmStatic
    @CheckResult
    @ButlerInternalApi
    internal fun provideNotifier(
        // Need to use MutableSet instead of Set because of Java -> Kotlin fun.
        @ButlerInternalApi dispatchers: MutableSet<NotifyDispatcher<*>>,
        context: Context
    ): Notifier {
      return Notifier.createDefault(context, dispatchers)
    }
  }
}
