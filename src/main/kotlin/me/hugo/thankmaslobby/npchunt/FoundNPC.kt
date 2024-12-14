package me.hugo.thankmaslobby.npchunt

import java.util.*

/**
 * Instance of a found npc with id [npcId] found by [finder].
 */
public data class FoundNPC(
    /** Id of the found NPC. */
    public val npcId: String,
    /** UUID of the player who found the NPC. */
    public val finder: UUID,
    /** Time at which this NPC was found. */
    public val timeFound: Long = System.currentTimeMillis(),
)