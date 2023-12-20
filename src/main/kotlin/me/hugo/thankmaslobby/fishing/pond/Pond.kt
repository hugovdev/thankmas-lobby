package me.hugo.thankmaslobby.fishing.pond

import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.extension.chooseWeighted
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.region.Region
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishType

/**
 * Fishing site that gives a rod to the player when entering
 * and sends them a message.
 */
public class Pond(
    public val pondId: String,
    public val name: String,
    public val description: String,
    private val enterMessage: String? = null,
    public var region: Region,
    private var fishWeights: Map<FishType, Double> = mapOf()
) : TranslatedComponent {

    init {
        val playerManager = ThankmasLobby.instance().playerManager

        this.region = region.toTriggering(
            onEnter = { player ->
                player.inventory.setItem(
                    2,
                    playerManager.getPlayerData(player.uniqueId).selectedRod.value.buildRod(player)
                )
                enterMessage?.let { player.sendTranslated(it) }
            },
            onLeave = { player ->
                player.inventory.setItem(2, null)
            })
    }

    /** @returns a random fish based on the config weights. */
    public fun catchFish(): FishType {
        return fishWeights.toList().chooseWeighted { it.second }.first
    }

}