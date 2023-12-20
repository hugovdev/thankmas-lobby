package me.hugo.thankmaslobby.game

import com.google.common.io.ByteStreams
import me.hugo.thankmas.config.string
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.state.StatefulValue
import me.hugo.thankmaslobby.ThankmasLobby
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player


/** A game that can be clicked in the game selector to be played. */
public class Game(
    public val name: String,
    public val serverName: String,
    public val item: TranslatableItem,
    public val slot: Int
) {

    public val playerCount: StatefulValue<Int> = StatefulValue(0)

    /** Constructor that reads a game from config. */
    public constructor(config: FileConfiguration, path: String) : this(
        config.string("$path.name"),
        config.string("$path.server-name"),
        TranslatableItem(config, "$path.icon"),
        config.getInt("$path.slot")
    )

    /** Sends [player] to the server for this game. */
    public fun send(player: Player) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF(serverName)
        player.sendPluginMessage(ThankmasLobby.instance(), "BungeeCord", out.toByteArray())
    }

}