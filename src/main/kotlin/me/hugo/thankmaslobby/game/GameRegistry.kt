package me.hugo.thankmaslobby.game

import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteStreams
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.ConfiguredMenu
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.Menu
import me.hugo.thankmas.items.addLoreTranslatable
import me.hugo.thankmas.player.translate
import me.hugo.thankmas.registry.MapBasedRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.extension.updateBoardTags
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Registry containing every game that can be played
 * or selected in the Game Selector.
 */
@Single
public class GameRegistry(config: FileConfiguration) : MapBasedRegistry<String, Game>(), KoinComponent,
    PluginMessageListener {

    public var globalPlayerCount: Int = 0
    public val gameSelector: Menu

    init {
        config.getKeys(false).forEach { register(it, Game(config, it)) }

        val configProvider: ConfigurationProvider by inject()

        gameSelector = ConfiguredMenu(configProvider.getOrLoad("menus"), "menus.game-selector").apply {
            getValues().forEach { game ->
                setIcon(game.slot, Icon({ context, _ ->
                    val clicker = context.clicker

                    clicker.closeInventory()
                    clicker.sendTranslated("game_selector.sending") { inserting("game", clicker.translate(game.name)) }

                    game.send(clicker)
                }) {
                    game.item.buildItem(it.locale()).addLoreTranslatable("game_selector.player_count", it.locale()) {
                        parsed("players", game.playerCount.value)
                    }
                }.listen(game.playerCount))
            }
        }

        Bukkit.getScheduler().runTaskTimer(ThankmasLobby.instance(), Runnable {
            val player = Bukkit.getOnlinePlayers().firstOrNull { it.isOnline } ?: return@Runnable

            getValues().forEach { player.requestServerCount(it.serverName) }
            player.requestServerCount("ALL")
        }, 0, 10)
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != "BungeeCord") return

        val input: ByteArrayDataInput = ByteStreams.newDataInput(message)

        val inputs = input.readUTF()
        val serverName = input.readUTF()
        val serverCount = input.readInt()

        if (!inputs.equals("PlayerCount", ignoreCase = true)) return

        getValues().firstOrNull { it.serverName == serverName }?.playerCount?.value = serverCount

        if (serverName.equals("all", ignoreCase = true)) {
            if (globalPlayerCount != serverCount) {
                globalPlayerCount = serverCount

                updateBoardTags("global_players")
            }
        }
    }

    /**
     * Sends a BungeeCord messaging channel requesting player
     * counts for [serverName].
     */
    private fun Player.requestServerCount(serverName: String) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("PlayerCount")
        out.writeUTF(serverName)

        sendPluginMessage(ThankmasLobby.instance(), "BungeeCord", out.toByteArray())
    }

}