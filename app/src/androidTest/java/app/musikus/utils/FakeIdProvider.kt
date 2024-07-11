package app.musikus.utils

import app.musikus.core.domain.IdProvider
import java.util.UUID

class FakeIdProvider : IdProvider {
    private var _currentId = 1L

    override fun generateId(): UUID {
        return UUID.fromString(
            "00000000-0000-0000-0000-${_currentId++.toString().padStart(12, '0')}"
        )
    }
}