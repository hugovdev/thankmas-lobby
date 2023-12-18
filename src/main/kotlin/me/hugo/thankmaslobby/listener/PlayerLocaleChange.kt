package me.hugo.thankmaslobby.listener

import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmaslobby.ThankmasLobby
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLocaleChangeEvent

/**
 * Listens to player language changes.
 */
public class PlayerLocaleChange(private val instance: ThankmasLobby) : Listener, TranslatedComponent {

    @EventHandler
    private fun onLocaleChange(event: PlayerLocaleChangeEvent) {
        val player = event.player

        // Online run when the player has already logged in and locale
        // is actually changing!
        if (!player.isOnline || event.locale() == player.locale()) return

        val playerData = instance.playerManager.getPlayerData(player.uniqueId)
        playerData.setTranslation(event.locale(), player)
    }

}