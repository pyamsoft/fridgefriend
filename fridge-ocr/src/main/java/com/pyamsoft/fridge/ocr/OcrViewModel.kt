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

import com.pyamsoft.fridge.ocr.OcrViewEvent.CameraError
import com.pyamsoft.fridge.ocr.OcrViewEvent.PreviewFrame
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class OcrViewModel @Inject internal constructor(
  private val interactor: OcrScannerInteractor
) : UiViewModel<OcrViewState, OcrViewEvent, OcrControllerEvent>(
    initialState = OcrViewState(text = "", throwable = null)
) {

  private var frameProcessDisposable by singleDisposable()

  override fun handleViewEvent(event: OcrViewEvent) {
    return when (event) {
      is PreviewFrame -> handlePreviewFrame(event)
      is CameraError -> handleError(event.cameraException)
    }
  }

  private fun handlePreviewFrame(frame: PreviewFrame) {
    if (!frameProcessDisposable.isDisposed) {
      return
    }

    frameProcessDisposable = interactor.processImage(
        frame.frameWidth,
        frame.frameHeight,
        frame.frameData.toByteArray(),
        frame.boundingTopLeft,
        frame.boundingWidth,
        frame.boundingHeight
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

  private fun handleError(throwable: Throwable) {
    setState { copy(throwable = throwable) }
  }
}
