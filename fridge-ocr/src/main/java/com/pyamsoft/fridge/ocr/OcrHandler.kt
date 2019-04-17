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

import com.pyamsoft.fridge.ocr.OcrHandler.OcrEvent
import com.pyamsoft.fridge.ocr.OcrHandler.OcrEvent.CameraError
import com.pyamsoft.fridge.ocr.OcrHandler.OcrEvent.PreviewFrame
import com.pyamsoft.fridge.ocr.OcrScannerView.Callback
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class OcrHandler @Inject internal constructor(
  bus: EventBus<OcrEvent>
) : UiEventHandler<OcrEvent, Callback>(bus),
  Callback {

  override fun onPreviewFrameReceived(
    frameWidth: Int,
    frameHeight: Int,
    frameData: ByteArray,
    boundingTopLeft: Pair<Int, Int>,
    boundingWidth: Int,
    boundingHeight: Int
  ) {
    publish(
      PreviewFrame(
        frameWidth,
        frameHeight,
        frameData,
        boundingTopLeft,
        boundingWidth,
        boundingHeight
      )
    )
  }

  override fun onCameraError(throwable: Throwable) {
    publish(CameraError(throwable))
  }

  override fun handle(delegate: Callback): Disposable {
    return listen()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe {
        return@subscribe when (it) {
          is CameraError -> delegate.onCameraError(it.throwable)
          is PreviewFrame -> delegate.onPreviewFrameReceived(
            it.frameWidth,
            it.frameHeight,
            it.frameData,
            it.boundingTopLeft,
            it.boundingWidth,
            it.boundingHeight
          )
        }
      }
  }

  sealed class OcrEvent {
    data class CameraError(val throwable: Throwable) : OcrEvent()
    data class PreviewFrame(
      val frameWidth: Int,
      val frameHeight: Int,
      val frameData: ByteArray,
      val boundingTopLeft: Pair<Int, Int>,
      val boundingWidth: Int,
      val boundingHeight: Int
    ) : OcrEvent() {

      override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        if (other is PreviewFrame) {
          if (frameWidth != other.frameWidth) return false
          if (frameHeight != other.frameHeight) return false
          if (!frameData.contentEquals(other.frameData)) return false
          if (boundingTopLeft != other.boundingTopLeft) return false
          if (boundingWidth != other.boundingWidth) return false
          if (boundingHeight != other.boundingHeight) return false
          return true
        }

        return false
      }

      override fun hashCode(): Int {
        var result = frameWidth
        result = 31 * result + frameHeight
        result = 31 * result + frameData.contentHashCode()
        result = 31 * result + boundingTopLeft.hashCode()
        result = 31 * result + boundingWidth
        result = 31 * result + boundingHeight
        return result
      }
    }
  }

}
