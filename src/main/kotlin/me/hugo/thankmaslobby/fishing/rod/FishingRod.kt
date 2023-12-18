package me.hugo.thankmaslobby.fishing.rod

import me.hugo.thankmas.items.TranslatableItem
import org.bukkit.entity.FishHook

public class FishingRod(
    private val id: String,
    private val item: TranslatableItem,
    private val maxFishTime: Int,
    private val minFishTime: Int,
    private val maxBiteTime: Int,
    private val minBiteTime: Int
) {

    /**
     * Applies the effects of this fishing rod to [hook].
     */
    public fun apply(hook: FishHook) {
        hook.apply {
            this.maxWaitTime = maxFishTime
            this.minWaitTime = minFishTime
            this.maxLureTime = maxBiteTime
            this.minLureTime = minBiteTime
        }
    }

}