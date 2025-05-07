package me.hugo.thankmaslobby.fishing.rod

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** All the captured fish and fish bag limit of a player. */
@Serializable
public data class PlayerFishingRods(
    public val fishingRods: MutableList<UnlockedRod> = mutableListOf(),
) {

    public operator fun contains(rod: String): Boolean = rod in fishingRods.map { it.fishingRodId }
    public operator fun contains(rod: FishingRod): Boolean = rod.id in fishingRods.map { it.fishingRodId }

}

/** Instance of an unlocked fishing rod. */
@Serializable
public data class UnlockedRod(
    /** Fishing rod id. */
    public val fishingRodId: String,
    /** Time at which this fishing rod was unlocked. */
    public val timeUnlocked: Instant = Clock.System.now(),
)