package app.musikus.core.domain

import java.util.UUID

interface IdProvider {
    fun generateId(): UUID
}

class IdProviderImpl : IdProvider {
    override fun generateId(): UUID {
        return UUID.randomUUID()
    }
}