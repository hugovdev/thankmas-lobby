package me.hugo.thankmaslobby

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.player.PlayerDataManager
import me.hugo.thankmas.region.RegionRegistry
import me.hugo.thankmaslobby.commands.LobbyCommands
import me.hugo.thankmaslobby.commands.ProfileMenuAccessor
import me.hugo.thankmaslobby.dependencyinjection.LobbyModules
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.pond.PondRegistry
import me.hugo.thankmaslobby.listener.PlayerAccess
import me.hugo.thankmaslobby.listener.PlayerCancelled
import me.hugo.thankmaslobby.listener.PlayerLocaleChange
import me.hugo.thankmaslobby.player.LobbyPlayer
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import org.koin.ksp.generated.module
import revxrsal.commands.bukkit.BukkitCommandHandler

public class ThankmasLobby : ThankmasPlugin() {


    public val playerManager: PlayerDataManager<LobbyPlayer> = PlayerDataManager { LobbyPlayer(it) }
    private val configProvider: ConfigurationProvider by inject()

    private val scoreboardManager: LobbyScoreboardManager by inject { parametersOf(this) }

    private val regionRegistry: RegionRegistry by inject { parametersOf(playerManager) }
    private val fishRegistry: FishTypeRegistry by inject()
    private val pondRegistry: PondRegistry by inject { parametersOf(configProvider.getOrLoad("ponds"), "ponds", this) }
    private val itemSetManager: ItemSetRegistry by inject { parametersOf(config) }

    private val profileMenuAccessor: ProfileMenuAccessor by inject { parametersOf(this) }

    private lateinit var commandHandler: BukkitCommandHandler

    override fun onEnable() {
        super.onEnable()
        saveDefaultConfig()

        loadKoinModules(LobbyModules().module)

        logger.info("Registering regions...")
        logger.info("Registered ${regionRegistry.size()} regions and started task!")

        logger.info("Registering fish types...")
        logger.info("Registered ${fishRegistry.size()} fish types!")

        logger.info("Registering ponds...")
        logger.info("Registered ${pondRegistry.size()} ponds!")

        logger.info("Registering item sets...")
        logger.info("Registered ${itemSetManager.size()} item sets!")

        scoreboardManager.initialize()

        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(PlayerAccess(this), this)
        pluginManager.registerEvents(PlayerLocaleChange(this), this)
        pluginManager.registerEvents(PlayerCancelled(), this)
        pluginManager.registerEvents(pondRegistry, this)

        commandHandler = BukkitCommandHandler.create(this)
        commandHandler.register(LobbyCommands(this))
        commandHandler.register(profileMenuAccessor)
        commandHandler.registerBrigadier()
    }

    override fun onDisable() {
        super.onDisable()
        commandHandler.unregisterAllCommands()
    }

}