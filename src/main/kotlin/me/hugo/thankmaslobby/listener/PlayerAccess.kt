package me.hugo.thankmaslobby.listener

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.reset
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.player.updateBoardTags
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.sql.SQLException

public class PlayerAccess(private val instance: ThankmasLobby) : Listener, TranslatedComponent {

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

        try {
            playerManager.createPlayerData(playerUUID)
        } catch (exception: SQLException) {
            exception.printStackTrace()

            event.loginResult = AsyncPlayerPreLoginEvent.Result.KICK_OTHER
            event.kickMessage(Component.text("Your data could not be loaded!", NamedTextColor.RED))
        }
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)

        updateBoardTags("players")

        val player = event.player

        player.isPersistent = false
        player.reset(GameMode.ADVENTURE)

        instance.playerManager.getPlayerData(player.uniqueId).setTranslation(player.locale())
    }

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId

        instance.playerManager.getPlayerData(playerId).save {
            instance.playerManager.removePlayerData(playerId)
        }

        event.quitMessage(null)
    }

    @EventHandler
    private fun onPlayerConnectionClose(event: PlayerConnectionCloseEvent) {
        updateBoardTags("players")
    }

}