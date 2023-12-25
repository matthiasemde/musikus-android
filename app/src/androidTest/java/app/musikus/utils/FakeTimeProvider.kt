package app.musikus.utils

import java.time.ZonedDateTime

class FakeTimeProvider : TimeProvider {
    private var _currentDateTime = ZonedDateTime.parse("1969-07-20T20:18:04.000Z")

    override fun now(): ZonedDateTime {
        _currentDateTime = _currentDateTime.plusSeconds(1)
        return _currentDateTime
    }

    fun setCurrentDateTime(dateTime: ZonedDateTime) {
        _currentDateTime = dateTime
    }
}