package me.hugo.thankmaslobby.scoreboard

import me.hugo.thankmas.scoreboard.ScoreboardTemplateManager
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.player.LobbyPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import org.koin.core.annotation.Single

/**
 * Loads and manages the lobby scoreboard.
 */
@Single
public class LobbyScoreboardManager(instance: ThankmasLobby) :
    ScoreboardTemplateManager<LobbyPlayer>(instance.playerManager) {

    override fun registerTags() {
        super.registerTags()

        registerTag("global_players") { Tag.selfClosingInserting { Component.text(0) } }

        registerTag("npcs") { Tag.selfClosingInserting { Component.text(0) } }
        registerTag("total_npcs") { Tag.selfClosingInserting { Component.text(0) } }

        registerTag("fishes") { player ->
            Tag.selfClosingInserting {
                Component.text(playerManager.getPlayerData(player.uniqueId).fishAmount())
            }
        }
    }

    override fun loadTemplates() {
        loadTemplate("scoreboard.lines", "lobby")
    }

}