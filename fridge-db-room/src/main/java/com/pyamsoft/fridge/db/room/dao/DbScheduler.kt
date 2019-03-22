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

package com.pyamsoft.fridge.db.room.dao

import androidx.annotation.CheckResult
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

@CheckResult
internal fun <T : Any> Single<T>.applyDbSchedulers(): Single<T> {
  return this
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.io())
    .unsubscribeOn(Schedulers.io())
}

@CheckResult
internal fun <T : Any> Observable<T>.applyDbSchedulers(): Observable<T> {
  return this
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.io())
    .unsubscribeOn(Schedulers.io())
}

@CheckResult
internal fun <T : Any> Flowable<T>.applyDbSchedulers(): Flowable<T> {
  return this
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.io())
    .unsubscribeOn(Schedulers.io())
}

@CheckResult
internal fun <T : Any> Maybe<T>.applyDbSchedulers(): Maybe<T> {
  return this
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.io())
    .unsubscribeOn(Schedulers.io())
}

@CheckResult
internal fun Completable.applyDbSchedulers(): Completable {
  return this
    .subscribeOn(Schedulers.io())
    .observeOn(Schedulers.io())
    .unsubscribeOn(Schedulers.io())
}
