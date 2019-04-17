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

package com.pyamsoft.fridge.ocr

import com.pyamsoft.fridge.ocr.OcrViewModel.OcrState
import com.pyamsoft.pydroid.arch.UiState
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class OcrViewModel @Inject internal constructor(
  private val interactor: OcrScannerInteractor
) : UiViewModel<OcrState>(
  initialState = OcrState(text = "", throwable = null)
), OcrScannerView.Callback {

  private var frameProcessDisposable by singleDisposable()

  override fun onBind() {
  }

  override fun onUnbind() {
    frameProcessDisposable.tryDispose()
  }

  override fun onPreviewFrameReceived(
    frameWidth: Int,
    frameHeight: Int,
    frameData: ByteArray,
    boundingTopLeft: Pair<Int, Int>,
    boundingWidth: Int,
    boundingHeight: Int
  ) {
    if (!frameProcessDisposable.isDisposed) {
      // These logs are noisy
//      Timber.w("Preview frame received but another process action is still in place.")
//      Timber.w("Dropping the received preview frame")
      return
    }

    frameProcessDisposable = interactor.processImage(
      frameWidth,
      frameHeight,
      frameData,
      boundingTopLeft,
      boundingWidth,
      boundingHeight
    )
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { frameProcessDisposable.tryDispose() }
      .subscribe({ handleOcrResult(it) }, {
        Timber.e(it, "Error processing preview frame for text")
        handleError(it)
      })
  }

  private fun handleOcrResult(text: String) {
    setState { copy(text = text) }
  }

  override fun onCameraError(throwable: Throwable) {
    handleError(throwable)
  }

  private fun handleError(throwable: Throwable) {
    setState { copy(throwable = throwable) }
  }

  data class OcrState(
    val text: String,
    val throwable: Throwable?
  ) : UiState
}