package me.hugo.thankmaslobby.listener

import dev.kezz.miniphrase.audience.sendTranslated
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
        val newLocale = event.locale()

        // Only run when the player has already logged in and locale
        // is actually changing!
        if (!player.isOnline || newLocale == player.locale()) return

        val playerData = instance.playerManager.getPlayerData(player.uniqueId)
        playerData.setTranslation(newLocale, player)
        player.sendTranslated("locale_changed", newLocale) {
            parsed("locale", newLocale.toLanguageTag())
        }
    }

}