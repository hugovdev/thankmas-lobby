package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.MiniPhraseContext
import dev.kezz.miniphrase.audience.sendTranslated
import kotlinx.datetime.Instant
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.hasKeyedData
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.firstIf
import me.hugo.thankmas.player.rank.RankedPlayerData
import me.hugo.thankmas.player.reset
import me.hugo.thankmas.state.StatefulValue
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.commands.ProfileMenuAccessor
import me.hugo.thankmaslobby.database.Fishes
import me.hugo.thankmaslobby.database.FoundNPCs
import me.hugo.thankmaslobby.database.PlayerData
import me.hugo.thankmaslobby.database.Rods
import me.hugo.thankmaslobby.fishing.fish.CaughtFish
import me.hugo.thankmaslobby.fishing.fish.FishType
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.npchunt.FoundNPC
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.koin.core.component.inject
import java.util.*


public class LobbyPlayer(playerUUID: UUID, instance: ThankmasLobby) :
    RankedPlayerData<LobbyPlayer>(playerUUID, instance.playerManager, belowNameSupplier = { _, locale ->
        ThankmasLobby.instance().translations.translations.translate("below_name_test", locale)
    }), TranslatedComponent {

    private val configProvider: ConfigurationProvider by inject()
    private val profileMenuAccessor: ProfileMenuAccessor by inject()

    private val fishRegistry: FishTypeRegistry by inject()
    private val rodRegistry: FishingRodRegistry by inject()

    /** @returns whether this player has expanded fish bag capacity. */
    private val hasExpandedBagCapacity
        get() = onlinePlayer.hasPermission("fishbag.expanded_capacity")

    /** @returns this player's fish bag capacity. */
    private val fishBagCapacity: Int
        get() = if (hasExpandedBagCapacity) 100 else 50

    private val foundNPCs: MutableMap<String, FoundNPC> = mutableMapOf()
    private val caughtFishes: MutableList<CaughtFish> = mutableListOf()

    /** The fishing rod this player is using to fish. */
    public lateinit var selectedRod: StatefulValue<FishingRod>
        private set

    public var lastHookShoot: Long = 0L

    /** List of the rods this player has unlocked. */
    public val unlockedRods: MutableMap<FishingRod, FishingRod.FishingRodData> = mutableMapOf()

    /** Menu that displays all the fishes the viewer has caught. */
    public val fishBag: PaginatedMenu =
        ConfigurablePaginatedMenu(
            configProvider.getOrLoad("hub/menus.yml"),
            "menus.fish-bag",
            profileMenuAccessor.fishingMenu.firstPage()
        )

    // Constructor is always run asynchronously, so we can load stuff from the database!
    init {
        val startTime = System.currentTimeMillis()
        val playerId = playerUUID.toString()

        transaction {
            val player = PlayerData.selectAll().where { PlayerData.uuid eq playerId }.singleOrNull()

            val rod: FishingRod = if (player != null) {
                rodRegistry.get(player[PlayerData.selectedRod])
            } else rodRegistry.getValues().first { it.tier == 1 }

            selectedRod = StatefulValue(rod).apply { subscribe { _, _, _ -> rebuildRod() } }

            // Load all the fishes this player has caught!
            FoundNPCs.selectAll().where { FoundNPCs.whoFound eq playerId }.forEach { result ->
                val npcId = result[FoundNPCs.npcId]
                foundNPCs[npcId] = FoundNPC(npcId, playerUUID, result[FoundNPCs.time].toEpochMilliseconds(), false)
            }

            // Load all the fishes this player has caught!
            Fishes.selectAll().where { Fishes.whoCaught eq playerId }.forEach { result ->
                val fishTypeId = result[Fishes.fishType]
                val fishType = fishRegistry.getOrNull(fishTypeId)

                if (fishType == null) {
                    ThankmasLobby.instance().logger.warning("Tried to find fish with id $fishTypeId, but doesn't exist!")
                    return@forEach
                }

                caughtFishes.add(
                    CaughtFish(
                        fishType,
                        playerUUID,
                        result[Fishes.pondId],
                        result[Fishes.time].toEpochMilliseconds(),
                        false
                    )
                )
            }

            // Load every rod this player has unlocked!
            Rods.selectAll().where { Rods.owner eq playerId }.forEach { result ->
                unlockedRods[rodRegistry.get(result[Rods.rodId])] =
                    FishingRod.FishingRodData(result[Rods.time].toEpochMilliseconds(), false)
            }

            // If the player has no rods then we give them the default one!
            if (unlockedRods.isEmpty()) {
                unlockedRods[rodRegistry.getValues().first { it.tier == 1 }] =
                    FishingRod.FishingRodData(System.currentTimeMillis())
            }
        }

        // Add caught fishes to the fish bag menu. Max at 150 for caution!
        caughtFishes.take(150).forEach { fishBag.addIcon(Icon { player -> it.buildItem(player) }) }
        instance.logger.info("Player data for $playerUUID loaded in ${System.currentTimeMillis() - startTime}ms.")
    }

    override fun setLocale(newLocale: Locale) {
        super.setLocale(newLocale)

        val finalPlayer = onlinePlayerOrNull ?: return

        val itemSetManager: ItemSetRegistry by inject()
        itemSetManager.giveSet("lobby", finalPlayer, newLocale)

        val currentBoard = lastBoardId ?: "lobby"

        val scoreboardManager: LobbyScoreboardManager by inject()
        scoreboardManager.getTemplate(currentBoard).printBoard(finalPlayer, newLocale)

        // If they are fishing also give them the new translated rod!
        rebuildRod(newLocale)
    }

    /** @returns whether this player found the NPC with id [npcId]. */
    public fun hasFound(npcId: String): Boolean = foundNPCs.containsKey(npcId)

    /** @returns the ids of every NPC found by this player. */
    public fun foundNPCs(): Set<String> = foundNPCs.keys

    /** Registers the finding of [npcId] for this player. */
    public fun find(npcId: String) {
        foundNPCs[npcId] = FoundNPC(npcId, playerUUID)
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

    protected override fun save() {
        val playerId = playerUUID.toString()

        transaction {
            // Update or insert this player's selected stuff!
            PlayerData.upsert {
                it[uuid] = playerId
                it[selectedRod] = this@LobbyPlayer.selectedRod.value.id
                it[selectedHat] = 0
            }

            // Insert all the recently unlocked NPCs!
            FoundNPCs.batchInsert(foundNPCs.values.filter { it.thisSession }) {
                this[FoundNPCs.whoFound] = playerId
                this[FoundNPCs.npcId] = it.npcId
                this[FoundNPCs.time] = Instant.fromEpochMilliseconds(it.timeFound)
            }

            // Insert the new fishes into the database!
            Fishes.batchInsert(caughtFishes.filter { it.thisSession }) {
                this[Fishes.whoCaught] = playerId
                this[Fishes.fishType] = it.fishType.id
                this[Fishes.pondId] = it.pondId
                this[Fishes.time] = Instant.fromEpochMilliseconds(it.timeCaptured)
            }

            // Insert the new unlocked rods into the database!
            Rods.batchInsert(unlockedRods.filter { it.value.thisSession }.toList()) {
                this[Rods.owner] = playerId
                this[Rods.rodId] = it.first.id
                this[Rods.time] = Instant.fromEpochMilliseconds(it.second.unlockTime)
            }
        }
    }

    context(MiniPhraseContext)
    override fun onPrepared(player: Player) {
        super.onPrepared(player)

        player.isPersistent = false
        player.reset(GameMode.ADVENTURE)

        val scoreboardManager: LobbyScoreboardManager by inject()
        scoreboardManager.getTemplate("lobby").printBoard(player)

        player.sendTranslated("welcome")

        // Give lobby item-set!
        val itemSetManager: ItemSetRegistry by inject()
        itemSetManager.giveSet("lobby", player)
    }
}