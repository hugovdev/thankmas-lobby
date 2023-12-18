package me.hugo.thankmaslobby.player

import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.player.ScoreboardPlayerData
import me.hugo.thankmaslobby.fishing.CapturedFish
import me.hugo.thankmaslobby.fishing.FishType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

public class LobbyPlayer(playerUUID: UUID) : ScoreboardPlayerData(playerUUID), KoinComponent {

    private val configProvider: ConfigurationProvider by inject()


    // private val unlockedNPCs: MutableList<EasterEggNPC> = mutableListOf()
    private val capturedFishes: MutableList<CapturedFish> = mutableListOf()

    /** Menu that displays all the fishes the viewer has caught. */
    public val fishBag: PaginatedMenu =
        ConfigurablePaginatedMenu(configProvider.getOrLoad("menus"), "menus.fish-bag").apply {
            capturedFishes.forEach { addIcon(Icon { player -> it.buildItem(player) }) }
        }

    init {
        // Load from SQL
    }

    /** Captures [fish] on [pondId]. */
    public fun captureFish(fish: FishType, pondId: String) {
        val capturedFish = CapturedFish(fish, playerUUID, pondId)

        capturedFishes.add(capturedFish)
        fishBag.addIcon(Icon { player -> capturedFish.buildItem(player) })
    }

    /** @returns the amount of captured fishes this player has. */
    public fun getCapturedFishes(): Int {
        return capturedFishes.size
    }

}