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

package com.pyamsoft.fridge.detail.scanner

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.pydroid.arch.BaseUiComponent
import javax.inject.Inject

internal class ScannerUiComponentImpl @Inject internal constructor(
  private val lookingLabel: OcrLookingLabel,
  private val scanner: OcrScannerView,
  private val presenter: OcrScannerPresenter
) : BaseUiComponent<ScannerUiComponent.Callback>(),
  ScannerUiComponent,
  LifecycleObserver,
  OcrScannerPresenter.Callback {

  private var lifecycleOwner: LifecycleOwner? = null

  override fun id(): Int {
    return scanner.id()
  }

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: ScannerUiComponent.Callback
  ) {
    owner.lifecycle.addObserver(this)
    lifecycleOwner = owner

    scanner.inflate(savedInstanceState)
    lookingLabel.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun onSaveState(outState: Bundle) {
    scanner.saveState(outState)
    lookingLabel.saveState(outState)
  }

  override fun onLayout(set: ConstraintSet) {
    lookingLabel.also {
      set.connect(it.id(), ConstraintSet.BOTTOM, scanner.id(), ConstraintSet.BOTTOM)
      set.connect(it.id(), ConstraintSet.START, scanner.id(), ConstraintSet.START)
      set.connect(it.id(), ConstraintSet.END, scanner.id(), ConstraintSet.END)
      set.constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
      set.constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
    }
  }

  @OnLifecycleEvent(ON_START)
  internal fun onStart() {
    scanner.start()
  }

  @OnLifecycleEvent(ON_STOP)
  internal fun onStop() {
    scanner.stop()
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun onDestroy() {
    lifecycleOwner?.lifecycle?.removeObserver(this)
    lifecycleOwner = null

    scanner.teardown()
    lookingLabel.teardown()
    presenter.unbind()
  }

  override fun handleOcrResult(text: String) {
    lookingLabel.lookingAt(text)
  }

  override fun handleCameraError(throwable: Throwable) {
    // TODO
  }

  override fun handleOcrError(throwable: Throwable) {
    // TODO
  }

}
