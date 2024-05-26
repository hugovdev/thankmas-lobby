package me.hugo.thankmaslobby.game

import com.google.common.io.ByteStreams
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.properties.InterfaceProperty
import com.noxcrew.interfaces.properties.interfaceProperty
import me.hugo.thankmas.config.string
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmaslobby.ThankmasLobby
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player


/** A game that can be clicked in the game selector to be played. */
public class Game(
    public val name: String,
    public val serverName: String,
    public val item: TranslatableItem,
    public val gridPoint: GridPoint
) {

    public val playerCount: InterfaceProperty<Int> = interfaceProperty(0)

    /** Constructor that reads a game from config. */
    public constructor(config: FileConfiguration, path: String) : this(
        config.string("$path.name"),
        config.string("$path.server-name"),
        TranslatableItem(config, "$path.icon"),
        GridPoint.at(config.getInt("$path.row"), config.getInt("$path.column"))
    )

    /** Sends [player] to the server for this game. */
    public fun send(player: Player) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF(serverName)
        player.sendPluginMessage(ThankmasLobby.instance(), "BungeeCord", out.toByteArray())
    }

}