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

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.CheckResult
import java.io.ByteArrayOutputStream

@CheckResult
fun Bitmap.toBytes(): ByteArray {
  val outStream = ByteArrayOutputStream()
  this.compress(PNG, 0, outStream)
  return outStream.toByteArray()
}

@CheckResult
fun ByteArray.toBitmap(width: Int, height: Int): Bitmap {
  val image = YuvImage(this, ImageFormat.NV21, width, height, null)
  val outStream = ByteArrayOutputStream()
  image.compressToJpeg(Rect(0, 0, width, height), 100, outStream)
  val bytes = outStream.toByteArray()
  return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}