package app.musikus.core.domain

import app.musikus.core.data.UUIDConverter
import java.util.UUID

class FakeIdProvider : IdProvider {
    private var _currentId = 1

    override fun generateId(): UUID {
        return UUIDConverter.fromInt(_currentId++)
    }
}