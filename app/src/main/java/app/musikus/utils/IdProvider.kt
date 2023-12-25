package app.musikus.utils

import java.util.UUID

interface IdProvider {
    fun generateId(): UUID
}

class IdProviderImpl : IdProvider {
    override fun generateId(): UUID {
        return UUID.randomUUID()
    }
}