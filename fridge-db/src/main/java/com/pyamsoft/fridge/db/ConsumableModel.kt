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

import androidx.annotation.CheckResult
import java.util.Date

interface ConsumableModel<T : Any> : BaseModel<T> {

  @CheckResult
  fun isConsumed(): Boolean

  @CheckResult
  fun consumptionDate(): Date?

  @CheckResult
  fun consume(date: Date): T

  @CheckResult
  fun isSpoiled(): Boolean

  @CheckResult
  fun spoiledDate(): Date?

  @CheckResult
  fun spoil(date: Date): T

}