package app.musikus.utils

import app.musikus.core.domain.TimeProvider
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class FakeTimeProvider : TimeProvider {
    private var _currentDateTime = START_TIME

    override fun now(): ZonedDateTime {
        return _currentDateTime
    }

    fun setCurrentDateTime(dateTime: ZonedDateTime) {
        _currentDateTime = dateTime
    }

    fun moveToTimezone(newZoneId: ZoneId) {
        _currentDateTime = _currentDateTime.withZoneSameInstant(newZoneId)
    }

    fun advanceTimeBy(duration: Duration) {
        _currentDateTime = _currentDateTime.plus(duration.toJavaDuration())
    }

    fun revertTimeBy(duration: Duration) {
        _currentDateTime = _currentDateTime.minus(duration.toJavaDuration())
    }

    companion object {
        val START_TIME: ZonedDateTime = ZonedDateTime.parse("1969-07-20T20:17:40Z[UTC]")
    }
}