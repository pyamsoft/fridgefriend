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

package com.pyamsoft.fridge.ui.chart

import android.graphics.Color
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter

class Pie private constructor(private val chart: PieChart) {

    fun setData(data: List<Data>) {
        setData(data, NOOP_FORMATTER)
    }

    inline fun setData(data: List<Data>, crossinline formatter: (Float) -> String) {
        setData(data, createFormatter(formatter))
    }

    @PublishedApi
    internal fun setData(data: List<Data>, formatter: ValueFormatter) {
        applyNewData(data, formatter)
    }

    private fun applyNewData(data: List<Data>, formatter: ValueFormatter) {
        val entries = data.map { PieEntry(it.value, "") }
        val colors = data.map { it.color }
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            this.valueFormatter = formatter
        }

        chart.data = PieData(dataSet)
        redrawPie()
    }

    private fun redrawPie() {
        chart.isDrawHoleEnabled = true
        chart.centerText = ""
        chart.description.text = ""
        chart.setDrawCenterText(false)
        chart.setHoleColor(Color.TRANSPARENT)
        chart.setDrawEntryLabels(false)
        chart.setDrawRoundedSlices(true)
        chart.setCenterTextColor(Color.TRANSPARENT)
        chart.setEntryLabelColor(Color.TRANSPARENT)
        chart.setTransparentCircleColor(Color.TRANSPARENT)
        chart.legend.isEnabled = false
        chart.animateX(300)
    }

    fun clear() {
        chart.clear()
    }

    data class Data(val value: Float, @ColorInt val color: Int)

    companion object {

        @JvmStatic
        @CheckResult
        @PublishedApi
        internal inline fun createFormatter(crossinline format: (Float) -> String): ValueFormatter {
            return object : ValueFormatter() {

                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    return format(value)
                }

                override fun getFormattedValue(value: Float): String {
                    return format(value)
                }

            }
        }

        @PublishedApi
        internal val NOOP_FORMATTER = createFormatter { "" }

        @JvmStatic
        @CheckResult
        fun fromChart(chart: PieChart): Pie {
            return Pie(chart)
        }
    }
}