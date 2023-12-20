package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.MiniPhraseContext
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
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
import me.hugo.thankmaslobby.fishing.fish.CaughtFish
import me.hugo.thankmaslobby.fishing.fish.FishType
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.inject
import java.sql.Timestamp
import java.util.*


public class LobbyPlayer(playerUUID: UUID, private val instance: ThankmasLobby) :
    RankedPlayerData(playerUUID, { player, locale ->
        Component.space()
            .append(Component.text("★", NamedTextColor.YELLOW))
            .append(Component.text("☆☆", NamedTextColor.GRAY))
    }),
    TranslatedComponent {

    private val configProvider: ConfigurationProvider by inject()
    private val profileMenuAccessor: ProfileMenuAccessor by inject()

    private val fishRegistry: FishTypeRegistry by inject()
    private val rodRegistry: FishingRodRegistry by inject()

    // private val unlockedNPCs: MutableList<EasterEggNPC> = mutableListOf()
    private val caughtFishes: MutableList<CaughtFish> = mutableListOf()

    /** The fishing rod this player is using to fish. */
    public val selectedRod: StatefulValue<FishingRod>

    /** List of the rods this player has unlocked. */
    public val unlockedRods: MutableMap<FishingRod, FishingRod.FishingRodData> = mutableMapOf()

    /** Menu that displays all the fishes the viewer has caught. */
    public val fishBag: PaginatedMenu =
        ConfigurablePaginatedMenu(
            configProvider.getOrLoad("menus"),
            "menus.fish-bag",
            profileMenuAccessor.fishingMenu.firstPage()
        )

    // Constructor is always run asynchronously, so we can load stuff from the database!
    init {
        val startTime = System.currentTimeMillis()

        instance.databaseConnector.getConnection().use {
            // Load base player data like selected rod and hat.
            it.prepareStatement("SELECT * FROM player_data WHERE uuid = ?").use { query ->
                val playerId = playerUUID.toString()
                query.setString(1, playerId)

                val results = query.executeQuery()

                val rod: FishingRod

                if (results.next()) {
                    rod = rodRegistry.get(results.getString("selected_rod"))
                    // TODO: hat stuff
                } else rod = rodRegistry.getValues().first { it.tier == 1 }

                selectedRod = StatefulValue(rod).apply { subscribe { _, _, _ -> rebuildRod() } }
            }

            // Load all the past fish caught.
            it.prepareStatement("SELECT * FROM fish_caught WHERE uuid = ?").use { query ->
                val playerId = playerUUID.toString()
                query.setString(1, playerId)

                val results = query.executeQuery()

                while (results.next()) {
                    caughtFishes.add(
                        CaughtFish(
                            fishRegistry.get(results.getString("fish_type")),
                            playerUUID,
                            results.getString("pond_id"),
                            results.getTimestamp("time").time,
                            false
                        )
                    )
                }
            }

            it.prepareStatement("SELECT * FROM unlocked_rods WHERE uuid = ?").use { query ->
                val playerId = playerUUID.toString()
                query.setString(1, playerId)

                val results = query.executeQuery()
                var hasRods = false

                while (results.next()) {
                    hasRods = true
                    unlockedRods[rodRegistry.get(results.getString("rod_id"))] =
                        FishingRod.FishingRodData(results.getTimestamp("time").time, false)
                }

                if (!hasRods) unlockedRods[rodRegistry.getValues().first { it.tier == 1 }] =
                    FishingRod.FishingRodData(System.currentTimeMillis())
            }
        }

        caughtFishes.take(150).forEach { fishBag.addIcon(Icon { player -> it.buildItem(player) }) }
        instance.logger.info("Player data for $playerUUID loaded in ${System.currentTimeMillis() - startTime}ms.")
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

        val playerManager = instance.playerManager

        Bukkit.getOnlinePlayers().forEach {
            // Update everyone's tags to the new language.
            playerManager.getPlayerDataOrNull(it.uniqueId)?.playerNameTag?.apply(finalPlayer, newLocale)
        }

        // If they are fishing also give them the new translated rod!
        rebuildRod(newLocale)
    }

    /** Captures [fish] on [pondId]. */
    public fun captureFish(fish: FishType, pondId: String) {
        val caughtFish = CaughtFish(fish, playerUUID, pondId)

        caughtFishes.add(caughtFish)
        fishBag.addIcon(Icon { player -> caughtFish.buildItem(player) })
    }

    /** @returns the amount of captured fishes this player has. */
    public fun fishAmount(): Int {
        return caughtFishes.size
    }

    /** @returns the amount of unique fish types this player has captured. */
    public fun uniqueFishTypes(): Int {
        return caughtFishes.groupBy { it.fishType }.size
    }

    /** Rebuilds the rod item and gives it to the player only if they already have one. */
    private fun rebuildRod(locale: Locale? = null) {
        val player = onlinePlayerOrNull ?: return

        val inventoryRod =
            player.inventory.firstIf { it.hasKeyedData(FishingRod.FISHING_ROD_ID, PersistentDataType.STRING) } ?: return

        player.inventory.setItem(inventoryRod.first, selectedRod.value.buildRod(player, locale))
    }

    public fun save(onSuccess: () -> Unit) {
        val startTime = System.currentTimeMillis()

        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable {
            val playerId = playerUUID.toString()

            instance.databaseConnector.getConnection().use { connection ->
                connection.prepareStatement("INSERT INTO player_data (`uuid`, `selected_rod`, `selected_hat`) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `selected_rod` = ?, `selected_hat` = ?")
                    .use {
                        it.setString(1, playerId)
                        it.setString(2, selectedRod.value.id)
                        it.setInt(3, 0)

                        it.setString(4, selectedRod.value.id)
                        it.setInt(5, 0)

                        it.execute()
                    }

                val newFishes = caughtFishes.filter { it.thisSession }

                if (newFishes.isNotEmpty()) {
                    connection.prepareStatement("INSERT INTO fish_caught VALUES(?, ?, ?, ?)").use { statement ->
                        newFishes.forEachIndexed { index, fish ->

                            statement.setString(1, playerId)
                            statement.setString(2, fish.fishType.id)
                            statement.setString(3, fish.pondId)
                            statement.setTimestamp(4, Timestamp(fish.timeCaptured))

                            statement.addBatch()

                            if (index % 1000 == 0) statement.executeBatch()
                        }

                        statement.executeBatch()
                    }
                }

                val newRods = unlockedRods.filter { it.value.thisSession }

                if (newRods.isNotEmpty()) {
                    connection.prepareStatement("INSERT INTO unlocked_rods VALUES(?, ?, ?)").use { statement ->
                        newRods.toList().forEachIndexed { index, rod ->

                            statement.setString(1, playerId)
                            statement.setString(2, rod.first.id)
                            statement.setTimestamp(3, Timestamp(rod.second.unlockTime))

                            statement.addBatch()

                            if (index % 1000 == 0) statement.executeBatch()
                        }

                        statement.executeBatch()
                    }
                }
            }

            Bukkit.getScheduler().runTask(instance, Runnable {
                onSuccess()
                instance.logger.info("Player info for $playerId saved and cleaned in ${System.currentTimeMillis() - startTime}ms.")
            })
        })
    }

}