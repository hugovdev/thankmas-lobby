package me.hugo.thankmaslobby.listener

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.reset
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.player.updateBoardTags
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Updates player counts, resets player states and
 * hides join and leave messages on join and quit.
 */
public class PlayerLobbyAccess : Listener, TranslatedComponent {

    private val playerManager
        get() = ThankmasLobby.instance().playerManager

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)

        updateBoardTags("players")

        val player = event.player

        player.isPersistent = false
        player.reset(GameMode.ADVENTURE)

        playerManager.getPlayerData(player.uniqueId).setTranslation(player.locale())
    }

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(null)

        playerManager.getPlayerData(event.player.uniqueId).removeAllHolograms()
    }

    @EventHandler
    private fun onPlayerConnectionClose(event: PlayerConnectionCloseEvent) {
        updateBoardTags("players")
    }
}