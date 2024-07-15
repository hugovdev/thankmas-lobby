package me.hugo.thankmaslobby.listener

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.markers.registry.MarkerRegistry
import me.hugo.thankmas.player.reset
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.player.updateBoardTags
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.inject
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.sql.SQLException

public class PlayerAccess(private val instance: ThankmasLobby) : Listener, TranslatedComponent {

    private val markerRegistry: MarkerRegistry by inject()

    private val spawnpoint: Location?
        get() = markerRegistry.getMarkerForType("hub_spawnpoint").firstOrNull()
            ?.location?.toLocation(ThankmasLobby.instance().hubWorld)

    @EventHandler
    private fun onPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        val playerUUID = event.uniqueId

        val playerManager = instance.playerManager
        val playerData = playerManager.getPlayerDataOrNull(playerUUID)

        if (playerData != null) {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                instance.globalTranslations.translate("general.kick.player_data_loaded")
            )

            // Save their old data to let them join.
            playerData.save { instance.playerManager.removePlayerData(playerUUID) }

            return
        }

        try {
            playerManager.createPlayerData(playerUUID)
        } catch (exception: SQLException) {
            exception.printStackTrace()

            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("Your data could not be loaded!", NamedTextColor.RED)
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onPlayerKicked(event: PlayerLoginEvent) {
        if (event.result == PlayerLoginEvent.Result.ALLOWED) return

        val playerManager = instance.playerManager

        if (playerManager.getPlayerDataOrNull(event.player.uniqueId) == null) return

        // Player got kicked while login. Whitelist? Full Server? Forget their data.
        playerManager.removePlayerData(event.player.uniqueId)
    }

    @EventHandler
    private fun onPlayerAllowed(event: PlayerLoginEvent) {
        if (event.result != PlayerLoginEvent.Result.ALLOWED) return

        val playerManager = instance.playerManager

        if (playerManager.getPlayerDataOrNull(event.player.uniqueId) != null) return

        // If the player went through pre-login but has no data, wtf? Kick them.
        event.disallow(
            PlayerLoginEvent.Result.KICK_OTHER,
            Component.text("Your data could not be loaded, please try again!", NamedTextColor.RED)
        )
    }

    @EventHandler
    private fun onSpawnDeciding(event: PlayerSpawnLocationEvent) {
        // Try to teleport the player to the hub_spawnpoint marker.
        spawnpoint?.let { event.spawnLocation = it }
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

    @EventHandler(priority = EventPriority.NORMAL)
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId
        val playerData = instance.playerManager.getPlayerData(playerId)

        playerData.removeAllHolograms()
        playerData.save { instance.playerManager.removePlayerData(playerId) }

        event.quitMessage(null)
    }

    @EventHandler
    private fun onPlayerConnectionClose(event: PlayerConnectionCloseEvent) {
        updateBoardTags("players")
    }

}