package me.hugo.thankmaslobby.fishing.pond

import me.hugo.thankmas.extension.chooseWeighted
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmaslobby.fishing.fish.FishType

/**
 * Fishing site that gives a rod to the player when entering
 * and sends them a message.
 */
public class Pond(
    public val pondId: String,
    public val name: String,
    public val description: String,
    public val enterMessage: String? = null,
    private var fishWeights: Map<FishType, Double> = mapOf()
) : TranslatedComponent {

    /** @returns a random fish based on the config weights. */
    public fun catchFish(): FishType {
        return fishWeights.toList().chooseWeighted { it.second }.first
    }

}