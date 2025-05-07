package me.hugo.thankmaslobby.npchunt

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Progress of all NPCs found and time of quest completion by player. */
@Serializable
public data class NPCFindQuestProgress(
    public val foundNPCs: MutableList<FoundNPC> = mutableListOf(),
    public val completionTime: Instant? = null
) {
    public operator fun contains(id: String): Boolean = id in foundNPCs.map { it.npcId }
}

/** Instance of a found npc with id [npcId], found at [timeFound]. */
@Serializable
public data class FoundNPC(
    /** Id of the found NPC. */
    public val npcId: String,
    /** Time at which this NPC was found. */
    public val timeFound: Instant = Clock.System.now()
)