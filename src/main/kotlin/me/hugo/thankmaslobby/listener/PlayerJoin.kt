package me.hugo.thankmaslobby.listener

import me.hugo.thankmaslobby.ThankmasLobby
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

public class PlayerJoin(private val instance: ThankmasLobby) : Listener {

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
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        instance.playerManager.removePlayerData(event.player.uniqueId)
    }

}