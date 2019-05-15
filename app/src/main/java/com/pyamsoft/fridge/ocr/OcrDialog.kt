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

//internal class OcrDialog : DialogFragment(), OcrUiComponent.Callback {
//
//  @JvmField @Inject internal var component: OcrUiComponent? = null
//
//  override fun onCreateView(
//    inflater: LayoutInflater,
//    container: ViewGroup?,
//    savedInstanceState: Bundle?
//  ): View? {
//    return inflater.inflate(R.layout.layout_constraint, container, false)
//  }
//
//  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    super.onViewCreated(view, savedInstanceState)
//
//    val parent = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
//    Injector.obtain<FridgeComponent>(view.context.applicationContext)
//      .plusScannerComponent()
//      .create(
//        parent,
//        requireArguments().getString(ITEM_ID, ""),
//        requireArguments().getString(ENTRY_ID, "")
//      )
//      .inject(this)
//
//    val component = requireNotNull(component)
//    component.bind(parent, viewLifecycleOwner, savedInstanceState, this)
//
//    parent.layout {
//      component.also {
//        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
//        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
//        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
//        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
//        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
//        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
//      }
//    }
//  }
//
//  override fun onResume() {
//    super.onResume()
//    dialog.window?.apply {
//      setLayout(
//        ViewGroup.LayoutParams.MATCH_PARENT,
//        ViewGroup.LayoutParams.MATCH_PARENT
//      )
//      setGravity(Gravity.CENTER)
//    }
//  }
//
//  override fun onSaveInstanceState(outState: Bundle) {
//    super.onSaveInstanceState(outState)
//    component?.saveState(outState)
//  }
//
//  override fun onDestroyView() {
//    super.onDestroyView()
//    component = null
//  }
//
//  companion object {
//
//    const val TAG = "OcrDialog"
//    private const val ITEM_ID = "item_id"
//    private const val ENTRY_ID = "entry_id"
//
//    @JvmStatic
//    @CheckResult
//    fun newInstance(item: FridgeItem): DialogFragment {
//      return OcrDialog().apply {
//        arguments = Bundle().apply {
//          putString(ITEM_ID, item.id())
//          putString(ENTRY_ID, item.entryId())
//        }
//      }
//    }
//  }
//
//}
