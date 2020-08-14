package org.kman.clearview.core

import org.kman.clearview.R

data class TimePeriod(val itemId: Int, val minutes: Int, val pointCount: Int) {
    companion object {

        fun buildTimeWindow(minutes: Int): RqTimeWindow {
            for (i in LIST) {
                if (i.minutes == minutes) {
                    val pointDuration = i.minutes * 60 / i.pointCount
                    return RqTimeWindow(i.pointCount, pointDuration)
                }
            }

            return RqTimeWindow(30, 60)
        }

        val LIST = arrayOf(
            TimePeriod(
                R.id.time_period_30_min, 30, 30
            ),
            TimePeriod(
                R.id.time_period_1_hour, 60, 30
            ),
            TimePeriod(
                R.id.time_period_2_hours, 2 * 60, 30
            ),
            TimePeriod(
                R.id.time_period_6_hours, 6 * 60, 30
            ),
            TimePeriod(
                R.id.time_period_12_hours, 12 * 60, 36
            ),
            TimePeriod(
                R.id.time_period_24_hours, 24 * 60, 24
            ),
            TimePeriod(
                R.id.time_period_4_days, 4 * 24 * 60, 32
            ),
            TimePeriod(
                R.id.time_period_2_weeks, 14 * 24 * 60, 28
            ),
            TimePeriod(
                R.id.time_period_6_weeks, 42 * 24 * 60, 42
            ),
            TimePeriod(
                R.id.time_period_3_months, 96 * 24 * 60, 32
            ),
            TimePeriod(
                R.id.time_period_6_months, 192 * 24 * 60, 32
            ),
            TimePeriod(
                R.id.time_period_1_year, 384 * 24 * 60, 32
            )
        )
    }
}
