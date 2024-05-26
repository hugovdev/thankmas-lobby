package me.hugo.thankmaslobby.fishing.pond

import me.hugo.thankmas.extension.chooseWeighted
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmaslobby.fishing.fish.FishType
import org.bukkit.Sound

/**
 * Fishing site that gives a rod to the player when entering
 * and sends them a message.
 */
public class Pond(
    public val pondId: String,
    public val enterSound: Sound,
    private var fishWeights: Map<FishType, Double> = mapOf(),
) : TranslatedComponent {

    /** @returns a random fish based on the config weights. */
    public fun catchFish(): FishType {
        return fishWeights.toList().chooseWeighted { it.second }.first
    }

}