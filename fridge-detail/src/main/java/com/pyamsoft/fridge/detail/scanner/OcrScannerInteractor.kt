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

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SparseIntArray
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.CheckResult
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.Builder
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

internal class OcrScannerInteractor @Inject internal constructor(
  private val context: Context,
  private val enforcer: Enforcer
) {

  private val windowManager by lazy {
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }

  private val sensorOrentation by lazy {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // On most devices, the sensor orientation is 90 degrees, but for some
    // devices it is 270 degrees. For devices with a sensor orientation of
    // 270, rotate : CameraManagerthe image an additional 180 ((270 + 270) % 360) degrees.
    val backCameraId = cameraManager.cameraIdList.filter {
      val facing = cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING)
      return@filter facing == CameraCharacteristics.LENS_FACING_BACK
    }.first()

    return@lazy cameraManager.getCameraCharacteristics(backCameraId)
      .get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
  }

  @CheckResult
  fun processImage(
    frameWidth: Int,
    frameHeight: Int,
    frameData: ByteArray,
    boundingTopLeft: Int,
    boundingWidth: Int,
    boundingHeight: Int
  ): Single<String> {
    return Single.create { emitter ->
      enforcer.assertNotOnMainThread()

      val bitmap = frameData.toBitmap(frameWidth, frameHeight)
      val cropped = cropBitmap(bitmap, boundingTopLeft, boundingWidth, boundingHeight)

      val metadata = Builder()
        .setWidth(cropped.width)
        .setHeight(cropped.height)
        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        .setRotation(convertToFirebaseRotation())
        .build()

      val image = FirebaseVisionImage.fromByteArray(cropped.toBytes(), metadata)
      val recognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
      recognizer.processImage(image)
        .addOnSuccessListener { visionText: FirebaseVisionText? ->
          if (visionText == null) {
            Timber.w("Firebase vision text was null")
            emitter.onSuccess("")
          } else {
            val text = visionText.text
            emitter.onSuccess(text)
          }
        }
        .addOnFailureListener {
          Timber.e(it, "Firebase vision text ocr failed")
          emitter.tryOnError(it)
        }

    }
  }

  @CheckResult
  private fun cropBitmap(
    bitmap: Bitmap,
    boundingTopLeft: Int,
    boundingWidth: Int,
    boundingHeight: Int
  ): Bitmap {
    return bitmap
  }

  // https://firebase.google.com/docs/ml-kit/android/recognize-text
  @CheckResult
  private fun convertToFirebaseRotation(): Int {
    enforcer.assertNotOnMainThread()

    // Get the device's current rotation relative to its "native" orientation.
    // Then, from the ORIENTATIONS table, look up the angle the image must be
    // rotated to compensate for the device's rotation.
    val deviceRotation = windowManager.defaultDisplay.rotation
    val compensation = ORIENTATIONS.get(deviceRotation)

    val rotationCompensation = (compensation + sensorOrentation + 270) % 360
    return when (rotationCompensation) {
      0 -> FirebaseVisionImageMetadata.ROTATION_0
      90 -> FirebaseVisionImageMetadata.ROTATION_90
      180 -> FirebaseVisionImageMetadata.ROTATION_180
      270 -> FirebaseVisionImageMetadata.ROTATION_270
      else -> {
        Timber.w("Bad rotation value: $rotationCompensation")
        FirebaseVisionImageMetadata.ROTATION_0
      }
    }
  }

  companion object {

    private val ORIENTATIONS = SparseIntArray().apply {
      append(Surface.ROTATION_0, 90)
      append(Surface.ROTATION_90, 0)
      append(Surface.ROTATION_180, 270)
      append(Surface.ROTATION_270, 180)
    }
  }

}