package me.hugo.thankmaslobby

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.player.PlayerDataManager
import me.hugo.thankmas.region.RegionRegistry
import me.hugo.thankmaslobby.commands.FishesCommand
import me.hugo.thankmaslobby.dependencyinjection.LobbyModules
import me.hugo.thankmaslobby.fishing.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.pond.PondRegistry
import me.hugo.thankmaslobby.listener.PlayerJoin
import me.hugo.thankmaslobby.player.LobbyPlayer
import org.bukkit.Bukkit
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf
import org.koin.ksp.generated.module
import revxrsal.commands.bukkit.BukkitCommandHandler

public class ThankmasLobby : ThankmasPlugin() {


    public val playerManager: PlayerDataManager<LobbyPlayer> = PlayerDataManager { LobbyPlayer(it) }
    private val configProvider: ConfigurationProvider by inject()

    private val regionRegistry: RegionRegistry by inject { parametersOf(playerManager) }
    private val fishRegistry: FishTypeRegistry by inject()
    private val pondRegistry: PondRegistry by inject { parametersOf(configProvider.getOrLoad("ponds"), "ponds") }

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

        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(PlayerJoin(this), this)

        commandHandler = BukkitCommandHandler.create(this)
        commandHandler.register(FishesCommand())
        commandHandler.registerBrigadier()
    }

    override fun onDisable() {
        super.onDisable()
        commandHandler.unregisterAllCommands()
    }

}