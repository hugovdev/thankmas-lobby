package me.hugo.thankmaslobby.player

import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** @returns every online player with an active scoreboard. */
public fun playersWithBoard(): List<Player> {
    return Bukkit.getOnlinePlayers()
        .filter { ThankmasLobby.instance().playerManager.getPlayerDataOrNull(it.uniqueId)?.getBoardOrNull() != null }
}

/** Updates this player's board lines that contains [tags]. */
public fun Player.updateBoardTags(vararg tags: String) {
    val scoreboardManager: LobbyScoreboardManager = ThankmasLobby.instance().scoreboardManager
    val playerData = ThankmasLobby.instance().playerManager.getPlayerData(uniqueId)

    playerData.getBoardOrNull() ?: return

    scoreboardManager.getTemplate(playerData.lastBoardId ?: "lobby").updateLinesForTag(this, *tags)
}

/** Updates this player's board lines that contains [tags]. */
public fun updateBoardTags(vararg tags: String) {
    playersWithBoard().forEach { it.updateBoardTags(*tags) }
}