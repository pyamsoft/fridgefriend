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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.fridge.ocr.OcrViewEvent.CameraError
import com.pyamsoft.fridge.ocr.OcrViewEvent.PreviewFrame
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.Snackbreak
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.Logger
import io.fotoapparat.selector.back
import io.fotoapparat.selector.lowestSensorSensitivity
import io.fotoapparat.view.CameraView
import timber.log.Timber
import javax.inject.Inject

class OcrScannerView @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup
) : BaseUiView<OcrViewState, OcrViewEvent>(parent) {

  override val layout: Int = R.layout.scanner

  override val layoutRoot by boundView<CameraView>(R.id.camera_view)

  // Interface with callback from the frameProcessor thread
  private val lock = Any()

  @Volatile private var fotoapparat: Fotoapparat? = null

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    createFotoApparat(view)
    owner.lifecycle.addObserver(object : LifecycleObserver {

      @Suppress("unused")
      @OnLifecycleEvent(ON_START)
      fun start() {
        synchronized(lock) {
          requireNotNull(fotoapparat).start()
        }
      }

      @Suppress("unused")
      @OnLifecycleEvent(ON_STOP)
      fun stop() {
        synchronized(lock) {
          requireNotNull(fotoapparat).stop()
        }
      }

      @Suppress("unused")
      @OnLifecycleEvent(ON_DESTROY)
      fun destroy() {
        owner.lifecycle.removeObserver(this)
      }

    })
  }

  private fun createFotoApparat(view: View) {
    fotoapparat = Fotoapparat(
        context = view.context.applicationContext,
        view = layoutRoot,
        focusView = null,
        lensPosition = back(),
        cameraErrorCallback = {
          Timber.e(it, "Critical camera error!")
          publish(CameraError(it))
        },
        logger = object : Logger {
          override fun log(message: String) {
            Timber.d(message)
          }
        },
        cameraConfiguration = CameraConfiguration(
            sensorSensitivity = lowestSensorSensitivity(),
            frameProcessor = { frame ->
              synchronized(lock) {
                val width = frame.size.width
                val height = frame.size.height
                val data = frame.image

                // This frame can fire while we are tearing down
                if (fotoapparat != null) {
                  publish(PreviewFrame(width, height, data.toList(), 0 to 0, 0, 0))
                }
              }
            }
        )
    )
  }

  override fun onRender(
    state: OcrViewState,
    oldState: OcrViewState?
  ) {
    state.throwable.let { throwable ->
      if (throwable == null) {
        clearError()
      } else {
        showError(throwable)
      }
    }
  }

  private fun clearError() {
    Snackbreak.bindTo(owner)
        .dismiss()
  }

  private fun showError(throwable: Throwable) {
    Snackbreak.bindTo(owner)
        .short(layoutRoot, throwable.message ?: "Unexpected error occurred")
        .show()
  }

  override fun onTeardown() {
    synchronized(lock) {
      fotoapparat = null
    }
  }

}
