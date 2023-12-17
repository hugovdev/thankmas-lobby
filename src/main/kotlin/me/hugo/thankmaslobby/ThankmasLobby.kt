package me.hugo.thankmaslobby

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.player.PlayerDataManager
import me.hugo.thankmaslobby.player.LobbyPlayer

public class ThankmasLobby : ThankmasPlugin() {

    public val playerManager: PlayerDataManager<LobbyPlayer> = PlayerDataManager { LobbyPlayer(it) }

    override fun onEnable() {
        super.onEnable()

    }

}