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

package com.pyamsoft.fridge.db

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.BaseDb.Delete
import com.pyamsoft.fridge.db.BaseDb.Insert
import com.pyamsoft.fridge.db.BaseDb.Query
import com.pyamsoft.fridge.db.BaseDb.Realtime

interface BaseDb<ChangeEvent : Any, R : Realtime<*>, Q : Query<*>, I : Insert<*>, D : Delete<*>> {

    suspend fun publish(event: ChangeEvent)

    @CheckResult
    fun realtime(): R

    @CheckResult
    fun queryDao(): Q

    @CheckResult
    fun insertDao(): I

    @CheckResult
    fun deleteDao(): D

    suspend fun invalidate()

    interface Realtime<T : Any> {

        suspend fun listenForChanges(onChange: suspend (event: T) -> Unit)
    }

    interface Delete<T : Any> {

        suspend fun delete(o: T)
    }

    interface Insert<T : Any> {

        suspend fun insert(o: T)
    }

    interface Query<T : Any> {

        @CheckResult
        suspend fun query(force: Boolean): List<T>
    }
}
