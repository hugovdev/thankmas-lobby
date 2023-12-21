package me.hugo.thankmaslobby

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.listener.PlayerNameTagUpdater
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
import me.hugo.thankmaslobby.listener.PlayerLocaleChange
import me.hugo.thankmaslobby.player.LobbyPlayer
import me.hugo.thankmaslobby.player.updateBoardTags
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import org.koin.ksp.generated.module
import revxrsal.commands.bukkit.BukkitCommandHandler

public class ThankmasLobby : ThankmasPlugin() {

    public val playerManager: PlayerDataManager<LobbyPlayer> = PlayerDataManager { LobbyPlayer(it, this) }
    private val configProvider: ConfigurationProvider by inject()

    public val scoreboardManager: LobbyScoreboardManager by inject { parametersOf(this) }
    private val regionRegistry: RegionRegistry by inject { parametersOf(playerManager) }

    // Fishing Stuff
    private val fishRegistry: FishTypeRegistry by inject()
    private val pondRegistry: PondRegistry by inject { parametersOf(configProvider.getOrLoad("ponds", "fishing/"), this) }
    private val rodsRegistry: FishingRodRegistry by inject { parametersOf(configProvider.getOrLoad("fishing_rods", "fishing/")) }

    private val gameRegistry: GameRegistry by inject { parametersOf(configProvider.getOrLoad("games")) }

    private val itemSetManager: ItemSetRegistry by inject { parametersOf(config) }

    private val profileMenuAccessor: ProfileMenuAccessor by inject { parametersOf(this) }

    public lateinit var databaseConnector: LobbyDatabase
    private lateinit var commandHandler: BukkitCommandHandler

    public companion object {
        private var instance: ThankmasLobby? = null

        public fun instance(): ThankmasLobby {
            val instance = instance
            requireNotNull(instance) { "Tried to fetch a ThankmasPlugin instance while it's null!" }

            return instance
        }
    }

    override fun onEnable() {
        super.onEnable()

        instance = this
        saveDefaultConfig()

        loadKoinModules(LobbyModules().module)

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
        databaseConnector = LobbyDatabase(configProvider.getOrLoad("database"))
        logger.info("Connected and created correctly!")

        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(PlayerAccess(this), this)
        pluginManager.registerEvents(PlayerLocaleChange(this), this)
        pluginManager.registerEvents(PlayerCancelled(), this)
        pluginManager.registerEvents(pondRegistry, this)
        pluginManager.registerEvents(PlayerNameTagUpdater(playerManager), this)

        // Register luck perms events!
        PlayerGroupChange(playerManager) { player -> player.updateBoardTags("rank") }

        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord");

        commandHandler = BukkitCommandHandler.create(this)
        rodsRegistry.registerCompletions(commandHandler)

        commandHandler.register(LobbyCommands(this))
        commandHandler.register(profileMenuAccessor)

        commandHandler.registerBrigadier()
    }

    override fun onDisable() {
        super.onDisable()

        databaseConnector.dataSource.close()
        commandHandler.unregisterAllCommands()
    }

}