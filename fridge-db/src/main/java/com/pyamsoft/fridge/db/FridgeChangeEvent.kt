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

sealed class FridgeChangeEvent {

  data class Insert(val item: FridgeItem) : FridgeChangeEvent()

  data class InsertGroup(val items: List<FridgeItem>) : FridgeChangeEvent()

  data class Update(val item: FridgeItem) : FridgeChangeEvent()

  data class UpdateGroup(val items: List<FridgeItem>) : FridgeChangeEvent()

  data class Delete(val id: String) : FridgeChangeEvent()

  data class DeleteGroup(val ids: List<String>) : FridgeChangeEvent()

  object DeleteAll : FridgeChangeEvent()

}
