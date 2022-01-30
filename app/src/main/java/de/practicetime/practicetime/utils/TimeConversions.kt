package de.practicetime.practicetime.utils

import java.util.*


const val SECONDS_PER_HOUR = 60 * 60
const val SECONDS_PER_DAY = 60 * 60 * 24

/** Format Time intelligently, e.g. "1h 30min" or "<1m" */
const val TIME_FORMAT_HUMAN_PRETTY = 0

/** Like TIME_FORMAT_HUMAN_PRETTY, but with "m" instead of "min" */
const val TIME_FORMAT_HUMAN_PRETTY_SHORT = 1

/** Format Time rounded to next full day / hour / minute / second, e.g. "22 hours", "32 minutes */
const val TIME_FORMAT_PRETTY_APPROX = 2

/** Fixed format HH:MM:SS */
const val TIME_FORMAT_HMS_DIGITAL = 3

/** Fixed format HH:MM for >1h, else e.g. "32 min" for <1h (used in GoalsProgressBar) */
const val TIME_FORMAT_HM_DIGITAL_OR_MIN_HUMAN = 4

fun secondsDurationToHoursMinSec(seconds: Int): Triple<Int, Int, Int> {
    val hours =  seconds / 3600
    val minutes = (seconds % 3600) / 60
    val seconds = seconds % 60

    return Triple(hours, minutes, seconds)
}

fun secondsDurationToFullDays(seconds: Int): Int {
    return seconds / SECONDS_PER_DAY
}

fun getDurationString(durationSeconds: Int, format: Int) : String {
    val (hours, minutes, seconds) = secondsDurationToHoursMinSec(durationSeconds)
    val days = secondsDurationToFullDays(durationSeconds)

    val minutesName = when(format) {
        TIME_FORMAT_HUMAN_PRETTY -> "min"
        TIME_FORMAT_HUMAN_PRETTY_SHORT -> "m"
        else -> ""
    }

    when (format) {
        TIME_FORMAT_HUMAN_PRETTY, TIME_FORMAT_HUMAN_PRETTY_SHORT -> {
            return if (hours > 0) {
                "%dh %d$minutesName".format(hours, minutes)
            } else if (minutes == 0 && durationSeconds > 0) {
                "<1$minutesName"
            } else {
                "%d$minutesName".format(minutes)
            }
        }

        TIME_FORMAT_HMS_DIGITAL -> {
            return "%02d:%02d:%02d".format(
                hours,
                minutes,
                seconds
            )
        }
        TIME_FORMAT_HM_DIGITAL_OR_MIN_HUMAN -> {
            return when {
                hours > 0 -> {
                    "%02d:%02d".format(hours, minutes)
                }
                minutes > 0 -> {
                    "%d min".format(minutes)
                }
                else -> {
                    "< 1min"
                }
            }
        }
        TIME_FORMAT_PRETTY_APPROX -> {
            return when {
                days > 1 -> {
                    // if time left is larger than a day, show the number of begun days
                    "${days + 1} days"
                }
                hours > 1 -> {
                    // show the number of begun hours
                    "${hours + 1} hours"
                }
                else -> {
                    // show the number of begun minutes
                    "${minutes + 1} minutes"
                }
            }
        }
        else -> {
            return "TIME_FORMAT_ERR"
        }
    }
}

fun getCurrTimestamp(): Long {
    return Date().time / 1000L
}