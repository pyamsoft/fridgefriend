/*
 * Copyright 2021 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.ui

import android.text.InputType
import android.widget.EditText
import androidx.annotation.CheckResult

@CheckResult
private fun isEditableType(inputType: Int): Boolean {
    return inputType != InputType.TYPE_NULL
}

val EditText.isEditable: Boolean
    @get:CheckResult get() {
        return isEditableType(inputType)
    }

fun EditText.setNotEditable() {
    setEditable(InputType.TYPE_NULL)
}

fun EditText.setEditable(inputType: Int) {
    val isEditable = isEditableType(inputType)
    this.inputType = inputType
    this.isFocusable = isEditable
    this.isLongClickable = isEditable
    this.setTextIsSelectable(isEditable)
    isFocusableInTouchMode = isEditable
    isFocusable = isEditable
    isCursorVisible = isEditable
}
