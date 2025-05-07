package me.hugo.thankmaslobby.fishing.fish

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** All the captured fish and fish bag limit of a player. */
@Serializable
public data class PlayerFishData(
    public val speciesFound: MutableMap<String, Instant> = mutableMapOf(),
    public val caughtFish: MutableList<CaughtFish> = mutableListOf(),
    public var size: Int = 120
) {

    public fun hasFound(type: FishType): Boolean = type.id in speciesFound

}

/** Instance of a captured fish. */
@Serializable
public data class CaughtFish(
    /** Type of fish that got caught. */
    public val fishTypeId: String,
    /** Pond where this fish was caught. */
    public val pondId: String,
    /** Time at which this fish was caught. */
    public val timeCaptured: Instant = Clock.System.now(),
)