package me.hugo.thankmaslobby

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.player.PlayerDataManager
import me.hugo.thankmaslobby.commands.FishesCommand
import me.hugo.thankmaslobby.dependencyinjection.LobbyModules
import me.hugo.thankmaslobby.player.LobbyPlayer
import org.koin.core.context.loadKoinModules
import org.koin.ksp.generated.module
import revxrsal.commands.bukkit.BukkitCommandHandler

public class ThankmasLobby : ThankmasPlugin() {

    public val playerManager: PlayerDataManager<LobbyPlayer> = PlayerDataManager { LobbyPlayer(it) }
    private lateinit var commandHandler: BukkitCommandHandler

    override fun onEnable() {
        super.onEnable()
        saveDefaultConfig()

        loadKoinModules(LobbyModules().module)

        commandHandler = BukkitCommandHandler.create(this)
        commandHandler.register(FishesCommand())
        commandHandler.registerBrigadier()
    }

    override fun onDisable() {
        super.onDisable()
        commandHandler.unregisterAllCommands()
    }

}