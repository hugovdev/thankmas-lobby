package me.hugo.thankmaslobby

import com.noxcrew.interfaces.InterfacesListeners
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.commands.TranslationsCommands
import me.hugo.thankmas.config.string
import me.hugo.thankmas.entity.HologramMarkerRegistry
import me.hugo.thankmas.entity.npc.PlayerNPCMarkerRegistry
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.listener.PlayerLocaleChange
import me.hugo.thankmas.listener.PlayerNameTagUpdater
import me.hugo.thankmas.listener.RankedPlayerChat
import me.hugo.thankmas.markers.registry.MarkerRegistry
import me.hugo.thankmas.player.PlayerDataManager
import me.hugo.thankmas.player.rank.PlayerGroupChange
import me.hugo.thankmas.region.RegionRegistry
import me.hugo.thankmaslobby.commands.LobbyCommands
import me.hugo.thankmaslobby.commands.ProfileMenuAccessor
import me.hugo.thankmaslobby.database.LobbyDatabase
import me.hugo.thankmaslobby.dependencyinjection.LobbyModules
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.pond.PondRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import me.hugo.thankmaslobby.game.GameRegistry
import me.hugo.thankmaslobby.listener.PlayerAccess
import me.hugo.thankmaslobby.listener.PlayerCancelled
import me.hugo.thankmaslobby.npchunt.NPCHuntListener
import me.hugo.thankmaslobby.player.LobbyPlayer
import me.hugo.thankmaslobby.player.updateBoardTags
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import org.koin.ksp.generated.module
import revxrsal.commands.bukkit.BukkitCommandHandler
import revxrsal.commands.ktx.SuspendFunctionsSupport

public class ThankmasLobby : ThankmasPlugin(listOf("hub")) {

    public val playerManager: PlayerDataManager<LobbyPlayer> = PlayerDataManager { LobbyPlayer(it, this) }

    public val scoreboardManager: LobbyScoreboardManager by inject { parametersOf(this) }
    private val regionRegistry: RegionRegistry by inject { parametersOf(playerManager) }

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

    private var worldName: String = "world"

    private val markerRegistry: MarkerRegistry by inject()
    private val gameRegistry: GameRegistry by inject { parametersOf(configProvider.getOrLoad("hub/games.yml")) }
    private val itemSetManager: ItemSetRegistry by inject { parametersOf(configProvider.getOrLoad("hub/config.yml")) }
    private val profileMenuAccessor: ProfileMenuAccessor by inject { parametersOf(this) }

    public lateinit var playerNPCRegistry: PlayerNPCMarkerRegistry<LobbyPlayer>
        private set

    private lateinit var databaseConnector: LobbyDatabase
    private lateinit var commandHandler: BukkitCommandHandler

    public companion object {
        private var instance: ThankmasLobby? = null

        public fun instance(): ThankmasLobby {
            return requireNotNull(instance)
            { "Tried to fetch a ThankmasPlugin instance while it's null!" }
        }
    }

    override fun onLoad() {
        super.onLoad()

        instance = this
        loadKoinModules(LobbyModules().module)

        val scopeWorld = configProvider.getOrLoad("hub/config.yml").string("world")

        Bukkit.unloadWorld(worldName, false)

        s3WorldSynchronizer.downloadWorld(
            scopeWorld,
            Bukkit.getWorldContainer().resolve(worldName).also { it.mkdirs() })
    }

    override fun onEnable() {
        super.onEnable()

        markerRegistry.loadWorldMarkers(this.worldName)

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

        logger.info("Registering item sets...")
        logger.info("Registered ${itemSetManager.size()} item sets!")

        scoreboardManager.initialize()

        logger.info("Creating Lobby Database connector and tables...")
        databaseConnector = LobbyDatabase(configProvider.getOrLoad("hub/database.yml"))
        logger.info("Connected and created correctly!")

        val pluginManager = Bukkit.getPluginManager()

        pluginManager.registerEvents(PlayerAccess(this), this)
        pluginManager.registerEvents(PlayerLocaleChange(playerManager), this)
        pluginManager.registerEvents(PlayerCancelled(), this)
        pluginManager.registerEvents(pondRegistry, this)
        pluginManager.registerEvents(PlayerNameTagUpdater(playerManager), this)
        pluginManager.registerEvents(HologramMarkerRegistry(worldName, playerManager), this)

        playerNPCRegistry = PlayerNPCMarkerRegistry(worldName, playerManager)

        pluginManager.registerEvents(playerNPCRegistry, this)
        pluginManager.registerEvents(NPCHuntListener(playerNPCRegistry), this)

        InterfacesListeners.install(this)

        // Check settings and ignored people etc.
        pluginManager.registerEvents(RankedPlayerChat(playerManager) { _, _ -> true }, this)

        // Register luck perms events!
        PlayerGroupChange(playerManager) { player -> player.updateBoardTags("rank") }

        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord");

        commandHandler = BukkitCommandHandler.create(this)
        commandHandler.accept(SuspendFunctionsSupport)

        rodsRegistry.registerCompletions(commandHandler)
        pondRegistry.registerCompletions(commandHandler)

        commandHandler.register(LobbyCommands(this))
        commandHandler.register(TranslationsCommands(playerManager))
        commandHandler.register(profileMenuAccessor)

        commandHandler.registerBrigadier()
    }

    override fun onDisable() {
        super.onDisable()

        databaseConnector.dataSource.close()
        commandHandler.unregisterAllCommands()
    }

    public val hubWorld: World
        get() = requireNotNull(Bukkit.getWorld(worldName)) { "Tried to use the main world before it was ready." }

}