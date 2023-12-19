package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.MiniPhraseContext
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.player.ScoreboardPlayerData
import me.hugo.thankmaslobby.commands.ProfileMenuAccessor
import me.hugo.thankmaslobby.fishing.fish.CapturedFish
import me.hugo.thankmaslobby.fishing.fish.FishType
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import java.util.UUID

public class LobbyPlayer(playerUUID: UUID) : ScoreboardPlayerData(playerUUID), KoinComponent {

    private val configProvider: ConfigurationProvider by inject()
    private val profileMenuAccessor: ProfileMenuAccessor by inject()

    private val rodRegistry: FishingRodRegistry by inject()

    // private val unlockedNPCs: MutableList<EasterEggNPC> = mutableListOf()
    private val capturedFishes: MutableList<CapturedFish> = mutableListOf()

    /** The fishing rod this player is using to fish. */
    public var selectedRod: FishingRod = rodRegistry.get("rusty_wooden_rod")
        private set

    /** List of the rods this player has unlocked. */
    public var unlockedRods: List<FishingRod> = listOf(selectedRod)

    /** Menu that displays all the fishes the viewer has caught. */
    public val fishBag: PaginatedMenu =
        ConfigurablePaginatedMenu(
            configProvider.getOrLoad("menus"),
            "menus.fish-bag",
            profileMenuAccessor.fishingMenu.firstPage()
        ).apply {
            capturedFishes.forEach { addIcon(Icon { player -> it.buildItem(player) }) }
        }

    init {
        // Load from SQL
    }

    context(MiniPhraseContext)
    public fun setTranslation(newLocale: Locale, player: Player? = null) {
        val finalPlayer = player ?: onlinePlayerOrNull ?: return

        // If we're initializing the board it's because the player just joined,
        // so we can also send them the join message!
        if (getBoardOrNull() == null) {
            initializeBoard("scoreboard.title", newLocale, player)
            finalPlayer.sendTranslated("welcome", newLocale)
        }

        val scoreboardManager: LobbyScoreboardManager by inject()
        val itemSetManager: ItemSetRegistry by inject()

        scoreboardManager.getTemplate("lobby").printBoard(finalPlayer, newLocale)
        itemSetManager.giveSet("lobby", finalPlayer, newLocale)
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