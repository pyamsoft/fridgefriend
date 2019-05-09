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

package com.pyamsoft.fridge.base

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyamsoft.fridge.R
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.theme.Theming

abstract class FridgeBottomSheetDialogFragment protected constructor() : BottomSheetDialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val theming = Injector.obtain<Theming>(requireActivity().applicationContext)
    val theme =
      if (theming.isDarkTheme()) R.style.Theme_Fridge_Dark_BottomSheetDialog else R.style.Theme_Fridge_Light_BottomSheetDialog
    return BottomSheetDialog(requireActivity(), theme)
  }

}
