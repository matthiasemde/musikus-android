/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.core.presentation.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class DurationFormat {

    /** Format Time intelligently, e.g. "1h 30m" or "< 1m" */
    HUMAN_PRETTY,

    /** Like TimeFormat.HUMAN_PRETTY, but without */
    HUMAN_PRETTY_SHORT,

    /** Format Time rounded to next full day / hour / minute / second, e.g. "22 hours", "32 minutes */
    PRETTY_APPROX,

    /** Same as TimeFormat.PRETTY_APPROX, but use "days", "h" and "m" instead of "days"/"hours"/"minutes" */
    PRETTY_APPROX_SHORT,

    /** Fixed format HH:MM:SS */
    HMS_DIGITAL,

    /** Fixed format MM:SS */
    MS_DIGITAL,

    /** Fixed format HH:MM for >1h, else e.g. "32 min" for <1h (used in GoalsProgressBar) */
    HM_DIGITAL_OR_MIN_HUMAN,

    /** Fixed format HH:MM:SS:CC (C = Centi seconds) */
    HMSC_DIGITAL,

    /** Fixed format MM:SS:CC (C = Centi seconds) */
    MSC_DIGITAL,
}

/** The scaling factor of 'h' an 'm' in time strings for smaller text. */
const val SCALE_FACTOR_FOR_SMALL_TEXT = 0.8f

typealias DurationString = AnnotatedString

/**
 * Get the formatted string for a duration of seconds, e.g. "1h 32m"
 *
 * The 'h' and 'm' are shrunk, if used. In that case,
 * the return value is a SpannableString which can be used directly in e.g. TextViews.
 * If you want to concat the returned CharSequence with another String, do it with
 * TextUtils.concat() to preserve styling!
 * If you NEED a String, simply call .toString().
 *
 * @param duration the duration in seconds to be converted
 * @param format one of TimeFormat.XXX variables, indicating the output format
 * @return A CharSequence (either a String or a SpannableString) with the final String.
 */

fun getDurationString(
    duration: Duration,
    format: DurationFormat,
): DurationString {
    val days = duration.inWholeDays
    var remainingDuration = duration - days.days

    val hours = (remainingDuration).inWholeHours
    val totalHours = days * 24 + hours // total hours, including days
    remainingDuration -= hours.hours

    val minutes = (remainingDuration).inWholeMinutes
    val totalMinutes = totalHours * 60 + minutes // total minutes, including days and hours
    remainingDuration -= minutes.minutes

    val seconds = (remainingDuration).inWholeSeconds
    remainingDuration -= seconds.seconds

    val milliseconds = remainingDuration.inWholeMilliseconds

    return buildAnnotatedString {
        when (format) {
            DurationFormat.HUMAN_PRETTY, DurationFormat.HUMAN_PRETTY_SHORT -> {
                val spaceOrNot = when (format) {
                    DurationFormat.HUMAN_PRETTY -> " "
                    DurationFormat.HUMAN_PRETTY_SHORT -> ""
                    else -> ""
                }

                if (totalHours > 0) {
                    append("$totalHours")
                    withStyle(SpanStyle(fontSize = SCALE_FACTOR_FOR_SMALL_TEXT.em)) {
                        append("h")
                    }
                }

                if (minutes > 0 || duration == 0.seconds) {
                    append("$spaceOrNot$minutes")
                    withStyle(SpanStyle(fontSize = SCALE_FACTOR_FOR_SMALL_TEXT.em)) {
                        append("m")
                    }
                }

                if (duration < 1.minutes && duration > 0.seconds) {
                    append("<${spaceOrNot}1")
                    withStyle(SpanStyle(fontSize = SCALE_FACTOR_FOR_SMALL_TEXT.em)) {
                        append("m")
                    }
                }
            }

            DurationFormat.HMSC_DIGITAL -> {
                append(
                    "%02d:%02d:%02d".format(
                        totalHours,
                        minutes,
                        seconds,
                    )
                )
                withStyle(SpanStyle(fontSize = 0.5.em)) {
                    append("%02d".format(milliseconds / 10))
                }
            }

            DurationFormat.MSC_DIGITAL -> {
                append(
                    "%02d:%02d".format(
                        totalMinutes,
                        seconds,
                    )
                )
                withStyle(SpanStyle(fontSize = 0.5.em)) {
                    append("%02d".format(milliseconds / 10))
                }
            }

            DurationFormat.HMS_DIGITAL -> {
                append(
                    "%02d:%02d:%02d".format(
                        totalHours,
                        minutes,
                        seconds,
                    )
                )
            }

            DurationFormat.MS_DIGITAL -> {
                append(
                    "%02d:%02d".format(
                        totalMinutes,
                        seconds,
                    )
                )
            }

            DurationFormat.HM_DIGITAL_OR_MIN_HUMAN -> {
                append(
                    when {
                        totalHours > 0 -> "%02d:%02d".format(totalHours, minutes)
                        minutes > 0 -> "%d min".format(minutes)
                        seconds > 0 -> "< 1min"
                        else -> "0 min"
                    }
                )
            }
            DurationFormat.PRETTY_APPROX -> {
                append(
                    when {
                        // if time left is at least one day, show the number of begun days
                        days > 0 -> "${days + 1} days"
                        // if days are zero, but hours is at least one, show the number of begun hours
                        hours > 0 -> "${hours + 1} hours"
                        // if the duration is less than one hour but still positive, show the number of begun minutes
                        duration.isPositive() -> "${minutes + 1} minutes"
                    // if duration is zero or negative, throw an error
                    else -> throw (IllegalArgumentException("Duration must be positive"))
                })
            }

            DurationFormat.PRETTY_APPROX_SHORT -> {
                append(
                    when {
                        // if time left is at least one day, show the number of begun days
                        days > 0 -> "${days + 1} days"
                        // if days are zero, but hours is at least one, show the number of begun hours
                        hours > 0 -> "${hours + 1}h"
                        // if the duration is less than one hour but still positive, show the number of begun minutes
                        duration.isPositive() -> "${minutes + 1}m"
                    // if the duration is zero or negative, throw an error
                    else -> throw (IllegalArgumentException("Duration must be positive"))
                })
            }
        }
    }
}
