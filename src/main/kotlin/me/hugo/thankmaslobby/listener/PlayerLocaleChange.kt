package me.hugo.thankmaslobby.listener

import me.hugo.thankmas.items.itemsets.ItemSetRegistry
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLocaleChangeEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Listens to player language changes.
 */
public class PlayerLocaleChange : Listener, KoinComponent {

    private val scoreboardManager: LobbyScoreboardManager by inject()
    private val itemSetManager: ItemSetRegistry by inject()

    @EventHandler
    private fun onLocaleChange(event: PlayerLocaleChangeEvent) {
        val player = event.player
        val newLocale = event.locale()

        scoreboardManager.getTemplate("lobby").printBoard(player, newLocale)
        itemSetManager.giveSet("lobby", player, newLocale)
    }

}