package me.hugo.thankmaslobby

import com.noxcrew.interfaces.InterfacesListeners
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.commands.CosmeticsCommand
import me.hugo.thankmas.commands.NPCCommands
import me.hugo.thankmas.commands.TranslationsCommands
import me.hugo.thankmas.cosmetics.CosmeticsRegistry
import me.hugo.thankmas.entity.HologramMarkerRegistry
import me.hugo.thankmas.entity.InteractionEntityRegistry
import me.hugo.thankmas.entity.npc.PlayerNPCMarkerRegistry
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.listener.*
import me.hugo.thankmas.player.PlayerDataManager
import me.hugo.thankmas.player.rank.PlayerGroupChange
import me.hugo.thankmas.player.updateBoardTags
import me.hugo.thankmas.region.RegionRegistry
import me.hugo.thankmas.region.types.HubJumpPad
import me.hugo.thankmas.region.types.MusicalRegion
import me.hugo.thankmaslobby.commands.LobbyCommands
import me.hugo.thankmaslobby.commands.ProfileMenuAccessor
import me.hugo.thankmaslobby.decoration.SummoningCircles
import me.hugo.thankmaslobby.dependencyinjection.LobbyModules
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.fish.PlayerFishData
import me.hugo.thankmaslobby.fishing.pond.PondRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.fishing.rod.PlayerFishingRods
import me.hugo.thankmaslobby.game.GameRegistry
import me.hugo.thankmaslobby.listener.PlayerLobbyAccess
import me.hugo.thankmaslobby.listener.PlayerLobbyProtection
import me.hugo.thankmaslobby.music.LobbyMusic
import me.hugo.thankmaslobby.npchunt.HubNPCListener
import me.hugo.thankmaslobby.npchunt.NPCFindQuestProgress
import me.hugo.thankmaslobby.player.LobbyPlayer
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.ksp.generated.module
import revxrsal.commands.bukkit.BukkitCommandHandler
import revxrsal.commands.ktx.SuspendFunctionsSupport
import java.util.*

public class ThankmasLobby :
    ThankmasPlugin<LobbyPlayer>(listOf("hub"), { listOf(LobbyModules().module) }) {

    override val playerDataManager: PlayerDataManager<LobbyPlayer> = PlayerDataManager { LobbyPlayer(it, this) }
    override val scoreboardTemplateManager: LobbyScoreboardManager by inject { parametersOf(this) }

    private val regionRegistry: RegionRegistry by inject()

    // Fishing Stuff
    private val fishRegistry: FishTypeRegistry by inject()
    private val pondRegistry: PondRegistry by inject {
        parametersOf(
            configProvider.getOrLoad("hub/fishing/ponds.yml"),
            this
        )
    }

    private val rodsRegistry: FishingRodRegistry by inject {
        parametersOf(configProvider.getOrLoad("hub/fishing/fishing_rods.yml"))
    }

    private val cosmeticsRegistry: CosmeticsRegistry by inject()

    override val worldNameOrNull: String = "world"

    private val gameRegistry: GameRegistry by inject { parametersOf(configProvider.getOrLoad("hub/games.yml")) }
    private val itemSetManager: ItemSetRegistry by inject { parametersOf(configProvider.getOrLoad("hub/config.yml")) }
    private val profileMenuAccessor: ProfileMenuAccessor by inject()

    public val playerNPCRegistry: PlayerNPCMarkerRegistry by inject {
        parametersOf(worldNameOrNull)
    }

    private lateinit var commandHandler: BukkitCommandHandler

    override fun onEnable() {
        super.onEnable()

        logger.info("Registering games...")
        logger.info("Registered ${gameRegistry.size()} games!")

        logger.info("Registering regions...")
        logger.info("Registered ${regionRegistry.size()} regions and started task!")

        logger.info("Registering fish types...")
        logger.info("Registered ${fishRegistry.size()} fish types!")

        logger.info("Registering ponds...")
        logger.info("Registered ${pondRegistry.size()} ponds!")

        logger.info("Registering fishing rods...")
        logger.info("Registered ${rodsRegistry.size()} fishing rods!")

        logger.info("Registering cosmetics...")
        logger.info("Registered ${cosmeticsRegistry.size()} cosmetics!")

        logger.info("Registering item sets...")
        logger.info("Registered ${itemSetManager.size()} item sets!")

        logger.info("Registered ${SummoningCircles(worldNameOrNull).summoningCircles.size} summoning circles!")

        this.scoreboardTemplateManager.initialize()

        val pluginManager = Bukkit.getPluginManager()

        // Player data loaders and spawnpoints
        pluginManager.registerEvents(PlayerDataLoader(this, this.playerDataManager), this)
        pluginManager.registerEvents(PlayerSpawnpointOnJoin(this, "hub_spawnpoint", 30), this)

        pluginManager.registerEvents(PlayerLocaleDetector(this.playerDataManager), this)
        pluginManager.registerEvents(PlayerAttributes("hub"), this)

        pluginManager.registerEvents(PlayerLobbyAccess(), this)
        pluginManager.registerEvents(PlayerLobbyProtection(), this)

        pluginManager.registerEvents(pondRegistry, this)
        pluginManager.registerEvents(HologramMarkerRegistry(this), this)
        pluginManager.registerEvents(playerNPCRegistry, this)
        pluginManager.registerEvents(HubNPCListener(playerNPCRegistry), this)
        pluginManager.registerEvents(InteractionEntityRegistry(this, mapOf(Pair("fish_sell") {
            fishRegistry.openSellMenu(it)
        })), this)

        InterfacesListeners.install(this)

        // Check settings and ignored people etc.
        pluginManager.registerEvents(RankedPlayerChat(this.playerDataManager), this)

        // Register luck perms events!
        PlayerGroupChange(this.playerDataManager) { player -> player.updateBoardTags("rank") }

        anvilWorldRegistry.getMarkerForType(worldNameOrNull, "hub_jump_pad").forEach {
            // Different ids so the region controller works properly.
            val jumpPadId = UUID.randomUUID().toString()

            regionRegistry.register(jumpPadId, HubJumpPad(it, jumpPadId, world))
        }

        anvilWorldRegistry.getMarkerForType(worldNameOrNull, "musical_region").forEach {
            // Different ids so the region controller works properly.
            val musicalRegionId = UUID.randomUUID().toString()

            regionRegistry.register(musicalRegionId, MusicalRegion(it, musicalRegionId, world))
        }

        val lobbyMusic: LobbyMusic by inject()
        lobbyMusic.runTaskTimer(this, 0L, 1L)

        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        server.messenger.registerIncomingPluginChannel(this, "BungeeCord", gameRegistry)

        commandHandler = BukkitCommandHandler.create(this)
        commandHandler.accept(SuspendFunctionsSupport)

        cosmeticsRegistry.registerCompletions(commandHandler)
        rodsRegistry.registerCompletions(commandHandler)
        pondRegistry.registerCompletions(commandHandler)

        commandHandler.register(LobbyCommands(this))
        commandHandler.register(TranslationsCommands(this.playerDataManager))
        commandHandler.register(CosmeticsCommand())
        commandHandler.register(profileMenuAccessor)
        commandHandler.register(NPCCommands())

        commandHandler.registerBrigadier()
    }

    override fun onDisable() {
        super.onDisable()

        commandHandler.unregisterAllCommands()
    }

    override fun initializeProperties() {
        super.initializeProperties()

        playerPropertyManager.initialize("player_fish_bags", { PlayerFishData() }, PlayerFishData.serializer())
        playerPropertyManager.initialize("player_fishing_rods", { PlayerFishingRods() }, PlayerFishingRods.serializer())
        playerPropertyManager.initialize(
            "player_npc_quests",
            { NPCFindQuestProgress() },
            NPCFindQuestProgress.serializer()
        )
    }
}