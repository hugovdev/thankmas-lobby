package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.audience.sendTranslated
import kotlinx.datetime.Instant
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.database.PlayerData
import me.hugo.thankmas.items.hasKeyedData
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.player.cosmetics.CosmeticsPlayerData
import me.hugo.thankmas.player.firstIf
import me.hugo.thankmas.player.reset
import me.hugo.thankmas.player.updateBoardTags
import me.hugo.thankmas.state.StatefulValue
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.database.FishUnlocked
import me.hugo.thankmaslobby.database.Fishes
import me.hugo.thankmaslobby.database.FoundNPCs
import me.hugo.thankmaslobby.database.Rods
import me.hugo.thankmaslobby.fishing.fish.CaughtFish
import me.hugo.thankmaslobby.fishing.fish.FishType
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.music.LobbyMusic
import me.hugo.thankmaslobby.npchunt.FoundNPC
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import java.util.*
import kotlin.collections.set

public class LobbyPlayer(playerUUID: UUID, instance: ThankmasLobby) :
    CosmeticsPlayerData<LobbyPlayer>(playerUUID, instance) {

    private val fishRegistry: FishTypeRegistry by inject()
    private val rodRegistry: FishingRodRegistry by inject()

    /** @returns whether this player has expanded fish bag capacity. */
    private val hasExpandedBagCapacity
        get() = onlinePlayer.hasPermission("fishbag.expanded_capacity")

    /** @returns this player's fish bag capacity. */
    private val fishBagCapacity: Int
        get() = if (hasExpandedBagCapacity) 200 else 120

    private val foundNPCs: MutableMap<String, FoundNPC> = mutableMapOf()
    public val caughtFishes: MutableList<CaughtFish> = mutableListOf()

    /** List of the rods this player has unlocked. */
    public val unlockedRods: MutableList<FishingRod> = mutableListOf()

    /** Different kinds of fish found by this player. */
    public val unlockedFish: MutableList<Pair<FishType, Long>> = mutableListOf()

    /** The fishing rod this player is using to fish. */
    public lateinit var selectedRod: StatefulValue<FishingRod>
        private set

    public var lastHookShoot: Long = 0L

    init {
        val startTime = System.currentTimeMillis()
        val playerId = playerUUID.toString()

        transaction {
            val playerData = PlayerData.selectAll().where { PlayerData.uuid eq playerId }.singleOrNull()

            loadCurrency(playerData)
            loadCosmetics(playerData)

            // Load every rod this player has unlocked!
            Rods.selectAll().where { Rods.owner eq playerId }.forEach { result ->
                unlockedRods += rodRegistry.get(result[Rods.rodId])
            }

            val rod: FishingRod = if (playerData != null) {
                val rodId = playerData[PlayerData.selectedRod]
                val databaseRod = rodRegistry.getOrNull(rodId)

                if (databaseRod != null && unlockedRods.contains(databaseRod)) {
                    databaseRod
                } else rodRegistry.getValues().first { it.tier == 1 }
            } else rodRegistry.getValues().first { it.tier == 1 }

            selectedRod = StatefulValue(rod).apply { subscribe { _, _, _ -> rebuildRod() } }

            // Load all the fishes this player has caught!
            FoundNPCs.selectAll().where { FoundNPCs.whoFound eq playerId }.forEach { result ->
                val npcId = result[FoundNPCs.npcId]
                foundNPCs[npcId] = FoundNPC(npcId, playerUUID, result[FoundNPCs.time].toEpochMilliseconds())
            }

            // Load all the unique fish types this player has caught!
            FishUnlocked.selectAll().where { FishUnlocked.whoCaught eq playerId }.forEach { result ->
                val fishTypeId = result[FishUnlocked.fishType]
                val fishType = fishRegistry.getOrNull(fishTypeId)

                if (fishType == null) {
                    ThankmasLobby.instance().logger.warning("Tried to find fish with id $fishTypeId, but doesn't exist!")
                    return@forEach
                }

                unlockedFish += Pair(fishType, result[FishUnlocked.time].toEpochMilliseconds())
            }

            // Load all the fishes this player has caught!
            Fishes.selectAll().where { Fishes.whoCaught eq playerId }.forEach { result ->
                val fishTypeId = result[Fishes.fishType]
                val fishType = fishRegistry.getOrNull(fishTypeId)

                if (fishType == null) {
                    ThankmasLobby.instance().logger.warning("Tried to find fish with id $fishTypeId, but doesn't exist!")
                    return@forEach
                }

                caughtFishes += CaughtFish(
                    fishType,
                    playerUUID,
                    result[Fishes.pondId],
                    result[Fishes.time].toEpochMilliseconds()
                )
            }

            // If the player has no rods then we give them the default one!
            if (unlockedRods.isEmpty()) {
                val freeRod = rodRegistry.getValues().first { it.tier == 1 }

                transaction {
                    Rods.insert {
                        it[owner] = playerUUID.toString()
                        it[rodId] = freeRod.id
                        it[time] = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                    }
                }

                unlockedRods += freeRod
            }
        }

        instance.logger.info("Player data for $playerUUID loaded in ${System.currentTimeMillis() - startTime}ms.")
    }

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

        updateBoardTags("players")
        giveCosmetic()

        val lobbyMusic: LobbyMusic by inject()
        lobbyMusic.playDefaultTrack(player)
    }

    override fun setLocale(newLocale: Locale) {
        super.setLocale(newLocale)

        val finalPlayer = onlinePlayerOrNull ?: return

        val itemSetManager: ItemSetRegistry by inject()
        itemSetManager.giveSet("lobby", finalPlayer, newLocale)

        val scoreboardManager: LobbyScoreboardManager by inject()
        scoreboardManager.getTemplate(lastBoardId).printBoard(finalPlayer, newLocale)

        // If they are fishing also give them the new translated rod!
        rebuildRod(newLocale)
    }

    protected override fun save() {
        val playerId = playerUUID.toString()

        transaction {
            // Update or insert this player's selected stuff!
            PlayerData.upsert {
                it[uuid] = playerId
                it[selectedRod] = this@LobbyPlayer.selectedRod.value.id
                it[selectedCosmetic] = this@LobbyPlayer.selectedCosmetic.value?.id ?: ""
                it[currency] = this@LobbyPlayer.currency
            }
        }
    }

    /** @returns whether this player found the NPC with id [npcId]. */
    public fun hasFound(npcId: String): Boolean = foundNPCs.containsKey(npcId)

    /** @returns the ids of every NPC found by this player. */
    public fun foundNPCs(): Set<String> = foundNPCs.keys

    /** Registers the finding of [npcId] for this player. */
    public fun find(npcId: String) {
        val foundNPC = FoundNPC(npcId, playerUUID)
        foundNPCs[npcId] = foundNPC

        Bukkit.getScheduler().runTaskAsynchronously(ThankmasLobby.instance(), Runnable {
            transaction {
                // Insert the new fish into the database!
                FoundNPCs.insert {
                    it[whoFound] = playerUUID.toString()
                    it[FoundNPCs.npcId] = npcId
                    it[time] = Instant.fromEpochMilliseconds(foundNPC.timeFound)
                }
            }
        })
    }

    /** Captures [fish] on [pondId]. */
    public fun captureFish(fish: FishType, pondId: String) {
        val caughtFish = CaughtFish(fish, playerUUID, pondId)
        caughtFishes += caughtFish

        Bukkit.getScheduler().runTaskAsynchronously(ThankmasLobby.instance(), Runnable {
            transaction {
                // Unlock new fish types!
                if (unlockedFish.none { it.first == fish }) {
                    unlockedFish += Pair(fish, caughtFish.timeCaptured)

                    FishUnlocked.insert {
                        it[whoCaught] = playerUUID.toString()
                        it[fishType] = fish.id
                        it[time] = Instant.fromEpochMilliseconds(caughtFish.timeCaptured)
                    }
                }

                // Insert the new fish into the database!
                Fishes.insert {
                    it[whoCaught] = playerUUID.toString()
                    it[fishType] = fish.id
                    it[Fishes.pondId] = pondId
                    it[time] = Instant.fromEpochMilliseconds(caughtFish.timeCaptured)
                }
            }
        })
    }

    /** Sells all fish of type [fish]. */
    public fun sellAllOfType(fish: FishType, soldFor: Int, onSold: () -> Unit = {}) {
        require(!inTransaction)
        require(caughtFishes.any { it.fishType == fish })

        val fishToRemove = caughtFishes.filter { it.fishType == fish }
        caughtFishes.removeAll(fishToRemove)

        val instance = ThankmasPlugin.instance()
        inTransaction = true

        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable {
            transaction {
                try {
                    transaction { Fishes.deleteWhere { (whoCaught eq playerUUID.toString()) and (fishType eq fish.id) } }

                    Bukkit.getScheduler().runTask(instance, Runnable {
                        currency += soldFor
                        onSold()

                        inTransaction = false
                    })
                } catch (e: Exception) {
                    rollback()

                    caughtFishes += fishToRemove
                    inTransaction = false

                    e.printStackTrace()
                }
            }
        })
    }

    /** Acquires [rod] for this player. */
    public fun acquireRod(rod: FishingRod, onAcquired: () -> Unit = {}) {
        require(rod !in unlockedRods)
        require(currency >= rod.price)
        require(!inTransaction)

        val instance = ThankmasPlugin.instance()

        inTransaction = true

        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable {
            transaction {
                Rods.insert {
                    it[owner] = playerUUID.toString()
                    it[rodId] = rod.id
                    it[time] = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                }
            }

            Bukkit.getScheduler().runTask(instance, Runnable {
                unlockedRods += rod
                currency -= rod.price

                onAcquired()

                inTransaction = false
            })
        })
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
            player.inventory.firstIf { it.hasKeyedData(FishingRod.FISHING_ROD_ID, PersistentDataType.STRING) }
                ?: return

        player.inventory.setItem(inventoryRod.first, selectedRod.value.buildRod(player, locale))
    }
}