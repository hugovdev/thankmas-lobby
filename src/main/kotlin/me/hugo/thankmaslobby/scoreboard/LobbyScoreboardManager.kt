package me.hugo.thankmaslobby.scoreboard

import me.hugo.thankmas.scoreboard.ScoreboardTemplateManager
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.game.GameRegistry
import me.hugo.thankmaslobby.player.LobbyPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.luckperms.api.LuckPermsProvider
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Loads and manages the lobby scoreboard.
 */
@Single
public class LobbyScoreboardManager(private val instance: ThankmasLobby) :
    ScoreboardTemplateManager<LobbyPlayer>(instance.playerManager), KoinComponent {

    private val gameRegistry: GameRegistry by inject()

    override fun registerTags() {
        super.registerTags()

        registerTag("global_players") { _, _ ->
            Tag.selfClosingInserting { Component.text(gameRegistry.globalPlayerCount) }
        }

        registerTag("npcs") { player, _ ->
            Tag.selfClosingInserting {
                Component.text(playerManager.getPlayerData(player.uniqueId).foundNPCs().size)
            }
        }

        registerTag("total_npcs") { _, _ ->
            Tag.selfClosingInserting {
                Component.text(
                    instance.playerNPCRegistry.getValues()
                        .filter { it.marker.getString("use") == "npc_hunt" }.size
                )
            }
        }

        registerTag("fishes") { player, _ ->
            Tag.selfClosingInserting {
                Component.text(playerManager.getPlayerData(player.uniqueId).fishAmount())
            }
        }

        registerTag("rank") { player, preferredLocale ->
            Tag.selfClosingInserting {
                playerManager.getPlayerData(player.uniqueId).getDecoratedRankName(preferredLocale ?: player.locale())
            }
        }
    }

    override fun loadTemplates() {
        loadTemplate("scoreboard.lines", "lobby")
    }

}