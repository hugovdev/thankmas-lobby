package me.hugo.thankmaslobby.scoreboard

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.scoreboard.ScoreboardTemplateManager
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.game.GameRegistry
import me.hugo.thankmaslobby.player.LobbyPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Loads and manages the lobby scoreboard.
 */
@Single
public class LobbyScoreboardManager(instance: ThankmasLobby) :
    ScoreboardTemplateManager<LobbyPlayer>(instance.playerManager), KoinComponent {

    private val gameRegistry: GameRegistry by inject()

    override fun registerTags() {
        super.registerTags()

        val luckPerms = LuckPermsProvider.get()
        val globalTranslations = ThankmasPlugin.instance().globalTranslations

        registerTag("global_players") { _, _ -> Tag.selfClosingInserting { Component.text(gameRegistry.globalPlayerCount) } }
        registerTag("npcs") { _, _ -> Tag.selfClosingInserting { Component.text(0) } }
        registerTag("total_npcs") { _, _ -> Tag.selfClosingInserting { Component.text(0) } }
        registerTag("fishes") { player, _ ->
            Tag.selfClosingInserting {
                Component.text(playerManager.getPlayerData(player.uniqueId).fishAmount())
            }
        }

        registerTag("rank") { player, preferredLocale ->
            val user = luckPerms.getPlayerAdapter(Player::class.java).getUser(player)

            Tag.selfClosingInserting {
                val group = user.primaryGroup

                globalTranslations.translate("rank.$group.name", preferredLocale ?: player.locale())
                    .color(
                        globalTranslations.translate("rank.$group.color", preferredLocale ?: player.locale()).color()
                    )
            }
        }
    }

    override fun loadTemplates() {
        loadTemplate("scoreboard.lines", "lobby")
    }

}