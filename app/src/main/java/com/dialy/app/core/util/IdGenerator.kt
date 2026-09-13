package com.dialy.app.core.util

import java.util.UUID

/**
 * Generates stable unique identifiers for domain items.
 */
object IdGenerator {
    fun generate(): String {
        return UUID.randomUUID().toString()
    }
}
