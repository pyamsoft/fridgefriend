/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.butler.work

import androidx.annotation.CheckResult

interface OrderParameters {

    @CheckResult
    fun getBooleanParameters(): Map<String, Boolean>

    // TODO(Peter): Support all data types

    class Builder {

        private val booleans = mutableMapOf<String, Boolean>()

        @CheckResult
        fun putBoolean(key: String, value: Boolean): Builder {
            booleans[key] = value
            return this
        }

        @CheckResult
        fun build(): OrderParameters {
            return object : OrderParameters {

                override fun getBooleanParameters(): Map<String, Boolean> {
                    return booleans.toMap()
                }

            }
        }

    }

    companion object {

        private val EMPTY = object : OrderParameters {
            override fun getBooleanParameters(): Map<String, Boolean> {
                return emptyMap()
            }
        }

        @JvmStatic
        @CheckResult
        fun empty(): OrderParameters {
            return EMPTY
        }
    }
}

@CheckResult
inline fun OrderParameters(block: OrderParameters.Builder.() -> Unit): OrderParameters {
    val builder = OrderParameters.Builder()
    builder.apply(block)
    return builder.build()
}
