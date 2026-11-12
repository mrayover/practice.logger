package com.mrayover.practicelogger

import androidx.room.withTransaction

/** Stable reviewed order. Visual references include the exclusively owned legacy original. */
internal suspend fun BlockDatabase.referenceOrder(block: String, role: MediaRole): List<String> {
    val canonical = blockMedia().list(block, role.name).map { it.position to "library:${it.mediaId}" }
    val old = if (role == MediaRole.VISUAL) images().find(block)?.let { listOf(it.position to "legacy:${it.fileName}") }.orEmpty() else emptyList()
    return (canonical + old).sortedWith(compareBy({ it.first }, { it.second })).map { it.second }
}

internal suspend fun BlockDatabase.writeReferenceOrder(block: String, role: MediaRole, order: List<String>) {
    order.forEachIndexed { position, identity ->
        if (identity.startsWith("library:")) blockMedia().move(block, role.name, identity.removePrefix("library:"), position)
        else images().move(block, position)
    }
}

internal suspend fun BlockDatabase.hasVisual(block: String, identity: String): Boolean = when {
    identity.startsWith("library:") -> blockMedia().contains(block, "VISUAL", identity.removePrefix("library:"))
    identity.startsWith("legacy:") -> images().find(block)?.fileName == identity.removePrefix("legacy:")
    else -> false
}

/** Scalar adapters replace only the reviewed source, keeping every other association and its order. */
internal suspend fun BlockDatabase.replaceVisual(block: String, media: String?, review: VisualChangeCheck) {
    val order = referenceOrder(block, MediaRole.VISUAL)
    val current = review.identity
    check(current == null || current in order) { "The visual reference changed. Review it again." }
    val identity = media?.let { "library:$it" }
    if (identity == current) return
    discardVisualMarkings(block, current, review)
    if (current?.startsWith("library:") == true) blockMedia().removeExact(block, "VISUAL", current.removePrefix("library:"))
    if (current?.startsWith("legacy:") == true) images().remove(block)
    if (media != null && !blockMedia().contains(block, "VISUAL", media)) blockMedia().save(BlockMediaLink(block, "VISUAL", media))
    val updated = order.toMutableList()
    val index = updated.indexOf(current)
    updated.remove(current)
    if (identity != null && identity !in updated) updated.add(if (index < 0) updated.size else index, identity)
    writeReferenceOrder(block, MediaRole.VISUAL, updated)
}
