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
import android.view.View
import android.view.ViewGroup
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.BaseUiView
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.Logger
import io.fotoapparat.selector.back
import io.fotoapparat.selector.lowestSensorSensitivity
import io.fotoapparat.view.CameraView
import timber.log.Timber
import javax.inject.Inject

internal class OcrScannerView @Inject internal constructor(
  parent: ViewGroup,
  callback: OcrScannerView.Callback
) : BaseUiView<OcrScannerView.Callback>(parent, callback) {

  override val layout: Int = R.layout.scanner

  override val layoutRoot by lazyView<CameraView>(R.id.camera_view)

  private var fotoapparat: Fotoapparat? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    fotoapparat = Fotoapparat(
      context = view.context.applicationContext,
      view = layoutRoot,
      focusView = null,
      lensPosition = back(),
      cameraErrorCallback = {
        Timber.e(it, "Critical camera error!")
        callback.onCameraError(it)
      },
      logger = object : Logger {
        override fun log(message: String) {
          Timber.d(message)
        }
      },
      cameraConfiguration = CameraConfiguration(
        sensorSensitivity = lowestSensorSensitivity(),
        frameProcessor = { frame ->
          val width = frame.size.width
          val height = frame.size.height
          val rotation = frame.rotation
          val data = frame.image
          callback.onPreviewFrameReceived(width, height, rotation, data)
        }
      )
    )
  }

  fun start() {
    requireNotNull(fotoapparat).start()
  }

  fun stop() {
    requireNotNull(fotoapparat).stop()
  }

  override fun onTeardown() {
    fotoapparat = null
  }

  interface Callback {

    fun onPreviewFrameReceived(width: Int, height: Int, rotation: Int, data: ByteArray)

    fun onCameraError(throwable: Throwable)

  }

}