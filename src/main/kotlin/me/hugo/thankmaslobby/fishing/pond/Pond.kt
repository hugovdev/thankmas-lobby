package me.hugo.thankmaslobby.fishing.pond

import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.region.Region
import me.hugo.thankmaslobby.fishing.FishType

/**
 * Fishing site that gives a rod to the player when entering
 * and sends them a message.
 */
public class Pond(
    private val pondId: String,
    private val name: String,
    private val description: String,
    private val enterMessage: String? = null,
    private val fishingRod: TranslatableItem,
    private var region: Region,
    private var fishWeights: Map<FishType, Double> = mapOf()
) : TranslatedComponent {

    init {
        this.region = region.toTriggering(
            onEnter = { player ->
                player.inventory.setItem(2, fishingRod.buildItem(player.locale()))
                enterMessage?.let { player.sendTranslated(it) }
            },
            onLeave = { player ->
                player.inventory.setItem(2, null)
            })
    }

}