package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.audience.sendTranslated
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.cosmetics.Cosmetic
import me.hugo.thankmas.database.PlayerData
import me.hugo.thankmas.items.hasKeyedData
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.player.PlayerBasics
import me.hugo.thankmas.player.cosmetics.CosmeticsPlayerData
import me.hugo.thankmas.player.firstIf
import me.hugo.thankmas.player.reset
import me.hugo.thankmas.player.updateBoardTags
import me.hugo.thankmas.state.StatefulValue
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.CaughtFish
import me.hugo.thankmaslobby.fishing.fish.FishType
import me.hugo.thankmaslobby.fishing.fish.PlayerFishData
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.fishing.rod.PlayerFishingRods
import me.hugo.thankmaslobby.fishing.rod.UnlockedRod
import me.hugo.thankmaslobby.music.LobbyMusic
import me.hugo.thankmaslobby.npchunt.FoundNPC
import me.hugo.thankmaslobby.npchunt.NPCFindQuestProgress
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.koin.core.component.inject
import java.util.*

public class LobbyPlayer(playerUUID: UUID, instance: ThankmasLobby) :
    CosmeticsPlayerData<LobbyPlayer>(playerUUID, instance) {

    private val rodRegistry: FishingRodRegistry by inject()

    private lateinit var fishData: PlayerFishData

    /** @returns a list of all the fish caught by this player. */
    public val caughtFishes: List<CaughtFish>
        get() = fishData.caughtFish.toList()

    /** @returns a list of all the unique fish this player found. */
    public val speciesFound: Map<String, Instant>
        get() = fishData.speciesFound.toMap()

    private lateinit var fishingRods: PlayerFishingRods

    /** @returns a list of all the rods this player has unlocked. */
    public val ownedRods: List<FishingRod>
        get() = fishingRods.fishingRods.map { rodRegistry.get(it.fishingRodId) }

    private lateinit var npcQuest: NPCFindQuestProgress

    /** The fishing rod this player is using to fish. */
    public lateinit var selectedRod: StatefulValue<FishingRod>
        private set

    public var lastHookShoot: Long = 0L

    override fun onLoading() {
        super.onLoading()

        fishData = playerPropertyManager.getProperty<PlayerFishData>().get(playerUUID)
        fishingRods = playerPropertyManager.getProperty<PlayerFishingRods>().get(playerUUID)

        val selectedRodId = basics.selectedRod

        val rod: FishingRod = if (selectedRodId.isNotBlank()) {
            rodRegistry.getOrNull(selectedRodId)?.takeIf { it in fishingRods }
                ?: rodRegistry.getValues().first { it.tier == 1 }
        } else rodRegistry.getValues().first { it.tier == 1 }

        selectedRod = StatefulValue(rod).apply { subscribe { _, _, _ -> rebuildRod() } }

        npcQuest = playerPropertyManager.getProperty<NPCFindQuestProgress>().get(playerUUID)
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
        transaction {
            // Update or insert this player's selected stuff!
            playerPropertyManager.getProperty<PlayerBasics>().write(playerUUID, basics)
        }
    }

    /** @returns whether this player found the NPC with id [npcId]. */
    public fun hasFound(npcId: String): Boolean = npcId in npcQuest

    /** @returns the ids of every NPC found by this player. */
    public fun foundNPCs(): List<String> = npcQuest.foundNPCs.map { it.npcId }

    /** Registers the finding of [npcId] for this player. */
    public fun find(npcId: String) {
        val foundNPC = FoundNPC(npcId)
        npcQuest.foundNPCs += foundNPC

        Bukkit.getScheduler().runTaskAsynchronously(ThankmasLobby.instance(), Runnable {
            transaction {
                playerPropertyManager.getProperty<NPCFindQuestProgress>().write(playerUUID, npcQuest)
            }
        })
    }

    /** Captures [fish] on [pondId]. */
    public fun captureFish(fish: FishType, pondId: String) {
        val fishTypeId = fish.id

        val caughtFish = CaughtFish(fishTypeId, pondId)

        fishData.caughtFish += caughtFish
        fishData.speciesFound[fishTypeId] = Clock.System.now()

        Bukkit.getScheduler().runTaskAsynchronously(ThankmasLobby.instance(), Runnable {
            transaction {
                playerPropertyManager.getProperty<PlayerFishData>().write(playerUUID, fishData)
            }
        })
    }

    /** Sells all fish of type [fish]. */
    public fun sellAllOfType(fish: FishType, soldFor: Int, onSold: () -> Unit = {}) {
        require(!inTransaction)

        val fishTypeId = fish.id
        require(fishData.caughtFish.any { it.fishTypeId == fishTypeId })

        val fishToRemove = fishData.caughtFish.filter { it.fishTypeId == fishTypeId }
        fishData.caughtFish.removeAll(fishToRemove)

        val instance = ThankmasPlugin.instance()
        inTransaction = true

        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable {
            transaction {
                try {
                    playerPropertyManager.getProperty<PlayerFishData>().write(playerUUID, fishData)

                    Bukkit.getScheduler().runTask(instance, Runnable {
                        currency += soldFor
                        onSold()

                        inTransaction = false
                    })
                } catch (e: Exception) {
                    rollback()

                    fishData.caughtFish += fishToRemove
                    inTransaction = false

                    e.printStackTrace()
                }
            }
        })
    }

    /** Acquires [rod] for this player. */
    public fun acquireRod(rod: FishingRod, onAcquired: () -> Unit = {}) {
        require(rod !in fishingRods)
        require(currency >= rod.price)
        require(!inTransaction)

        val instance = ThankmasPlugin.instance()

        inTransaction = true
        fishingRods.fishingRods += UnlockedRod(rod.id)

        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable {
            transaction { playerPropertyManager.getProperty<PlayerFishingRods>().write(playerUUID, fishingRods) }

            Bukkit.getScheduler().runTask(instance, Runnable {
                currency -= rod.price
                onAcquired()
                inTransaction = false
            })
        })
    }

    // A bunch of useful contains operators to quickly check if a player owns a rod, cosmetic, fish, etc.
    public operator fun contains(rod: FishingRod): Boolean = rod in fishingRods
    public operator fun contains(cosmetic: Cosmetic): Boolean = cosmetic in wardrobe
    public operator fun contains(fish: FishType): Boolean = fish.id in fishData.speciesFound

    /** @returns the amount of captured fishes this player has. */
    public fun fishAmount(): Int {
        return fishData.caughtFish.size
    }

    /** @returns the amount of species this player has found. */
    public fun fishSpeciesFound(): Int {
        return fishData.speciesFound.size
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