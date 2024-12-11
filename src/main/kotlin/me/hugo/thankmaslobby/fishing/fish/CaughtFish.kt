package me.hugo.thankmaslobby.fishing.fish

import me.hugo.thankmas.lang.Translated
import java.util.*

/**
 * Instance of a captured fish of type [fishType] owned by [catcher].
 */
public data class CaughtFish(
    /** Type of fish that got caught. */
    public val fishType: FishType,
    /** UUID of the player who caught this fish. */
    public val catcher: UUID,
    /** Pond where this fish was caught. */
    public val pondId: String,
    /** Time at which this fish was caught. */
    public val timeCaptured: Long = System.currentTimeMillis(),
    /**
     * Weather or not this fish was caught in this player
     * session and needs to be saved in the database.
     */
    public val thisSession: Boolean = true
) : Translated