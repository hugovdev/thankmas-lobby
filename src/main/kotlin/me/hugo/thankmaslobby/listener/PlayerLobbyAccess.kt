package me.hugo.thankmaslobby.listener

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.updateBoardTags
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/** Updates player counts hides join and leave messages on join and quit. */
public class PlayerLobbyAccess : Listener, TranslatedComponent {

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(null)
    }

    @EventHandler
    private fun onPlayerConnectionClose(event: PlayerConnectionCloseEvent) {
        updateBoardTags("players")
    }
}