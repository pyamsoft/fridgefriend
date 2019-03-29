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

import com.pyamsoft.fridge.detail.DetailScope
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import timber.log.Timber
import javax.inject.Inject

@DetailScope
internal class OcrScannerPresenter @Inject internal constructor(

) : BasePresenter<Unit, OcrScannerPresenter.Callback>(RxBus.empty()),
  OcrScannerView.Callback {

  private var frameProcessDisposable by singleDisposable()

  override fun onBind() {
  }

  override fun onUnbind() {
  }

  override fun onPreviewFrameReceived(width: Int, height: Int, rotation: Int, data: ByteArray) {
    if (!frameProcessDisposable.isDisposed) {
      Timber.w("Preview frame received but another process action is still in place.")
      Timber.w("Dropping the received preview frame")
      return
    }
  }

  override fun onCameraError(throwable: Throwable) {
    callback.handleCameraError(throwable)
  }

  interface Callback {

    fun handleCameraError(throwable: Throwable)

  }

}