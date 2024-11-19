package me.hugo.thankmaslobby.game

import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteStreams
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.ChestInterface
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.buildConfiguredChestInterface
import me.hugo.thankmas.items.addLoreTranslatable
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.translate
import me.hugo.thankmas.player.updateBoardTags
import me.hugo.thankmas.registry.MapBasedRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.player.isDonor
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.koin.core.annotation.Single
import org.koin.core.component.inject

/**
 * Registry containing every game that can be played
 * or selected in the Game Selector.
 */
@Single
public class GameRegistry(config: FileConfiguration) : MapBasedRegistry<String, Game>(), TranslatedComponent,
    PluginMessageListener {

    public var globalPlayerCount: Int = 0
    public val gameSelector: ChestInterface

    init {
        config.getKeys(false).forEach { register(it, Game(config, it)) }

        val configProvider: ConfigurationProvider by inject()

        gameSelector = buildConfiguredChestInterface(configProvider.getOrLoad("hub/menus.yml"), "menus.game-selector") {
            getValues().forEach { game ->
                withTransform(game.playerCount) { pane, a ->
                    val playerCount by game.playerCount

                    pane[game.gridPoint] = StaticElement(
                        drawable(game.item.buildItem(a.player.locale())
                            .addLoreTranslatable("game_selector.player_count", a.player.locale()) {
                                parsed("players", playerCount)
                            })
                    ) {
                        val clicker = it.player

                        clicker.closeInventory()

                        if (clicker.isDonor("perk.play_games")) {
                            clicker.sendTranslated("game_selector.sending") {
                                inserting("game", clicker.translate(game.name))
                            }

                            game.send(clicker)
                        }
                    }
                }
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

        var count by getValues().firstOrNull { it.serverName == serverName }?.playerCount ?: return
        count = serverCount

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