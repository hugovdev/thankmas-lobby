package me.hugo.thankmaslobby.fishing

import java.util.UUID

/**
 * Instance of a captured fish of type [fishType] owned by [catcher].
 */
public data class CapturedFish(
    public val fishType: FishType,
    public val catcher: UUID,
    public val pondId: String,
    public val timeCaptured: Long = System.currentTimeMillis(),
)