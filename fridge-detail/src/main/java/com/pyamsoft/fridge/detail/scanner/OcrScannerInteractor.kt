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

import androidx.annotation.CheckResult
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

internal class OcrScannerInteractor @Inject internal constructor(
  private val enforcer: Enforcer
) {

  @CheckResult
  fun processImage(width: Int, height: Int, rotation: Int, data: ByteArray): Single<String> {
    return Single.create { emitter ->
      enforcer.assertNotOnMainThread()

      val metadata = FirebaseVisionImageMetadata.Builder()
        .setWidth(width)
        .setHeight(height)
        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        .setRotation(convertToFirebaseRotation(rotation))
        .build()

      val image = FirebaseVisionImage.fromByteArray(data, metadata)
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
  private fun convertToFirebaseRotation(rotation: Int): Int {
    return when (rotation) {
      90 -> FirebaseVisionImageMetadata.ROTATION_90
      180 -> FirebaseVisionImageMetadata.ROTATION_180
      270 -> FirebaseVisionImageMetadata.ROTATION_270
      else -> FirebaseVisionImageMetadata.ROTATION_0
    }
  }

}