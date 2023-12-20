package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.MiniPhraseContext
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.StatefulIcon
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.hasKeyedData
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.firstIf
import me.hugo.thankmas.player.rank.RankedPlayerData
import me.hugo.thankmas.state.StatefulValue
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.commands.ProfileMenuAccessor
import me.hugo.thankmaslobby.fishing.fish.CapturedFish
import me.hugo.thankmaslobby.fishing.fish.FishType
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.inject
import java.util.*

public class LobbyPlayer(playerUUID: UUID) :
    RankedPlayerData(playerUUID, { player, locale ->
        Component.space()
            .append(Component.text("★", NamedTextColor.YELLOW))
            .append(Component.text("☆☆", NamedTextColor.GRAY))
    }),
    TranslatedComponent {

    private val configProvider: ConfigurationProvider by inject()
    private val profileMenuAccessor: ProfileMenuAccessor by inject()

    private val rodRegistry: FishingRodRegistry by inject()

    // private val unlockedNPCs: MutableList<EasterEggNPC> = mutableListOf()
    private val capturedFishes: MutableList<CapturedFish> = mutableListOf()

    /** The fishing rod this player is using to fish. */
    public val selectedRod: StatefulValue<FishingRod> = StatefulValue(rodRegistry.get("rusty_wooden_rod")).apply {
        subscribe { _, _, _ -> rebuildRod() }
    }

    /** List of the rods this player has unlocked. */
    public val unlockedRods: MutableList<FishingRod> = rodRegistry.getValues().toMutableList()

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

        val itemSetManager: ItemSetRegistry by inject()
        itemSetManager.giveSet("lobby", finalPlayer, newLocale)

        val currentBoard = lastBoardId ?: "lobby"

        val scoreboardManager: LobbyScoreboardManager by inject()
        scoreboardManager.getTemplate(currentBoard).printBoard(finalPlayer, newLocale)

        val playerManager = ThankmasLobby.instance().playerManager

        Bukkit.getOnlinePlayers().forEach {
            // Update everyone's tags to the new language.
            playerManager.getPlayerDataOrNull(it.uniqueId)?.playerNameTag?.apply(finalPlayer, newLocale)
        }

        // If they are fishing also give them the new translated rod!
        rebuildRod(newLocale)
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

    /** Rebuilds the rod item and gives it to the player only if they already have one. */
    private fun rebuildRod(locale: Locale? = null) {
        val player = onlinePlayer

        val inventoryRod =
            player.inventory.firstIf { it.hasKeyedData(FishingRod.FISHING_ROD_ID, PersistentDataType.STRING) } ?: return

        player.inventory.setItem(inventoryRod.first, selectedRod.value.buildRod(player, locale))
    }

}