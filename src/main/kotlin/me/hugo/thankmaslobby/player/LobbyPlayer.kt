package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.MiniPhraseContext
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.player.ScoreboardPlayerData
import me.hugo.thankmaslobby.fishing.CapturedFish
import me.hugo.thankmaslobby.fishing.FishType
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
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

    context(MiniPhraseContext)
    public fun reloadTranslations(newLocale: Locale) {
        val player = onlinePlayer

        // If we're initializing the board it's because the player just joined,
        // so we can also send them the join message!
        if (getBoardOrNull() == null) {
            initializeBoard("scoreboard.title", newLocale)
            player.sendTranslated("welcome", newLocale)
        }

        val scoreboardManager: LobbyScoreboardManager by inject()
        val itemSetManager: ItemSetRegistry by inject()

        scoreboardManager.getTemplate("lobby").printBoard(player, newLocale)
        itemSetManager.giveSet("lobby", player, newLocale)
    }

    /** Captures [fish] on [pondId]. */
    public fun captureFish(fish: FishType, pondId: String) {
        val capturedFish = CapturedFish(fish, playerUUID, pondId)

        capturedFishes.add(capturedFish)
        fishBag.addIcon(Icon { player -> capturedFish.buildItem(player) })
    }

    /** @returns the amount of captured fishes this player has. */
    public fun fishAmount(): Int {
        return capturedFishes.size
    }

    /** @returns the amount of unique fish types this player has captured. */
    public fun uniqueFishTypes(): Int {
        return capturedFishes.groupBy { it.fishType }.size
    }

}