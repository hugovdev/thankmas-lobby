package me.hugo.thankmaslobby.listener

import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.reset
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

public class PlayerAccess(private val instance: ThankmasLobby) : Listener, TranslatedComponent {

    private val scoreboardManager: LobbyScoreboardManager by inject { parametersOf(instance) }
    private val itemSetManager: ItemSetRegistry by inject()

    @EventHandler
    private fun onPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        val playerUUID = event.uniqueId

        val playerManager = instance.playerManager
        val playerData = playerManager.getPlayerDataOrNull(playerUUID)

        if (playerData != null) {
            event.loginResult = AsyncPlayerPreLoginEvent.Result.KICK_OTHER
            event.kickMessage(instance.globalTranslations.translate("general.kick.player_data_loaded"))

            return
        }

        playerManager.createPlayerData(playerUUID)
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)

        val player = event.player

        player.isPersistent = false
        player.reset(GameMode.ADVENTURE)

        instance.playerManager.getPlayerData(player.uniqueId).setTranslation(player.locale())
    }

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        instance.playerManager.removePlayerData(event.player.uniqueId)

        event.quitMessage(null)
    }

}