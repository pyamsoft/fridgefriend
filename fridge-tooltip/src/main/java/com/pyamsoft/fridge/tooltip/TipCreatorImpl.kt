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
 *
 */

package com.pyamsoft.fridge.tooltip

import android.app.Activity
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner

internal abstract class TipCreatorImpl<B : Any, R : Tip> protected constructor(
    activity: Activity
) : BalloonBuilderCreator(activity), TipCreator<B, R> {

    private val empty: B.() -> B = { this }

    @CheckResult
    protected abstract fun build(owner: LifecycleOwner, builder: B.() -> B): BalloonCreator

    @CheckResult
    protected abstract fun create(creator: BalloonCreator, direction: TipDirection): R

    final override fun center(owner: LifecycleOwner): R {
        return center(owner, empty)
    }

    final override fun center(owner: LifecycleOwner, builder: B.() -> B): R {
        return create(build(owner, builder), TipDirection.CENTER)
    }

    final override fun top(owner: LifecycleOwner): R {
        return top(owner, empty)
    }

    final override fun top(owner: LifecycleOwner, builder: B.() -> B): R {
        return create(build(owner, builder), TipDirection.TOP)
    }

    final override fun left(owner: LifecycleOwner): R {
        return left(owner, empty)
    }

    final override fun left(owner: LifecycleOwner, builder: B.() -> B): R {
        return create(build(owner, builder), TipDirection.LEFT)
    }

    final override fun right(owner: LifecycleOwner): R {
        return right(owner, empty)
    }

    final override fun right(owner: LifecycleOwner, builder: B.() -> B): R {
        return create(build(owner, builder), TipDirection.RIGHT)
    }

    final override fun bottom(owner: LifecycleOwner): R {
        return bottom(owner, empty)
    }

    final override fun bottom(owner: LifecycleOwner, builder: B.() -> B): R {
        return create(build(owner, builder), TipDirection.BOTTOM)
    }
}
