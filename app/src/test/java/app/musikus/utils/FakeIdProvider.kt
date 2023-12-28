package app.musikus.utils

import java.util.UUID

fun intToUUID(value: Int): UUID {
    return UUID.fromString(
        "00000000-0000-0000-0000-${value.toString().padStart(12, '0')}"
    )
}

class FakeIdProvider : IdProvider {
    private var _currentId = 1

    override fun generateId(): UUID {
        return intToUUID(_currentId++)
    }
}