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

package com.pyamsoft.fridge.detail.snackbar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.pyamsoft.fridge.detail.databinding.DetailCustomSnackbarBinding
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener

class CustomSnackbar private constructor(
    parent: ViewGroup,
    binding: DetailCustomSnackbarBinding,
) : BaseTransientBottomBar<CustomSnackbar>(parent, binding.root, binding.root) {

    @CheckResult
    private fun getBinding(): DetailCustomSnackbarBinding {
        val layout = view.getChildAt(0) as CustomSnackbarLayout
        return layout.binding
    }

    /**
     * Update the text in this [CustomSnackbar].
     *
     * @param message The new text for this [CustomSnackbar].
     */
    @CheckResult
    fun setText(message: CharSequence): CustomSnackbar {
        val tv = getBinding().snackbarText
        tv.text = message
        return this
    }

    /**
     * Update the text in this [CustomSnackbar].
     *
     * @param resId The new text for this [CustomSnackbar].
     */
    @CheckResult
    fun setText(@StringRes resId: Int): CustomSnackbar {
        return setText(context.getText(resId))
    }

    /**
     * Set the action to be displayed in this [CustomSnackbar].
     *
     * @param resId String resource to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    @CheckResult
    fun setAction1(@StringRes resId: Int, listener: View.OnClickListener?): CustomSnackbar {
        return setAction1(context.getText(resId), listener)
    }

    /**
     * Set the action to be displayed in this [CustomSnackbar].
     *
     * @param text Text to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    @CheckResult
    fun setAction1(text: CharSequence?, listener: View.OnClickListener?): CustomSnackbar {
        val tv: TextView = getBinding().snackbarAction1
        return setAction(tv, text, listener)
    }

    /**
     * Set the action to be displayed in this [CustomSnackbar].
     *
     * @param resId String resource to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    @CheckResult
    fun setAction2(@StringRes resId: Int, listener: View.OnClickListener?): CustomSnackbar {
        return setAction2(context.getText(resId), listener)
    }

    /**
     * Set the action to be displayed in this [CustomSnackbar].
     *
     * @param text Text to display for the action
     * @param listener callback to be invoked when the action is clicked
     */
    @CheckResult
    fun setAction2(text: CharSequence?, listener: View.OnClickListener?): CustomSnackbar {
        val tv: TextView = getBinding().snackbarAction2
        return setAction(tv, text, listener)
    }

    @CheckResult
    private fun setAction(
        tv: TextView,
        text: CharSequence?,
        listener: View.OnClickListener?,
    ): CustomSnackbar {
        if (text.isNullOrBlank() || listener == null) {
            tv.isGone = true
            tv.setOnDebouncedClickListener(null)
        } else {
            tv.isVisible = true
            tv.text = text
            tv.setOnDebouncedClickListener { view ->
                listener.onClick(view)
                // Now dismiss the Snackbar
                dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION)
            }
        }
        return this
    }

    companion object {

        @JvmField
        val Break = Snackbreak.create { view, message, duration ->
            create(view, message, duration)
        }

        // Copied from Snackbar.java
        @JvmStatic
        @CheckResult
        private fun findSuitableParent(startPoint: View): ViewGroup? {
            var view: View? = startPoint
            var fallback: ViewGroup? = null

            do {
                if (view is CoordinatorLayout) {
                    // We've found a CoordinatorLayout, use it
                    return view
                } else if (view is FrameLayout) {
                    if (view.id == android.R.id.content) {
                        // If we've hit the decor content view, then we didn't find a CoL in the
                        // hierarchy, so use the decor view.
                        return view
                    }

                    // It's not the content view but we'll use it as our fallback
                    fallback = view
                }

                if (view != null) {
                    // Else, we will loop and crawl up the view hierarchy and try to find a parent
                    val parent = view.parent
                    view = if (parent is View) parent else null
                }
            } while (view != null)

            // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
            return fallback
        }

        @JvmStatic
        @CheckResult
        fun create(view: View, message: CharSequence, duration: Int): CustomSnackbar {
            val parent = findSuitableParent(view)
                ?: throw IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view.")

            val context = parent.context

            val inflater = LayoutInflater.from(context)
            val binding = DetailCustomSnackbarBinding.inflate(inflater, parent, false)

            return CustomSnackbar(parent, binding)
                .setText(message)
                .setDuration(duration)
        }
    }

}