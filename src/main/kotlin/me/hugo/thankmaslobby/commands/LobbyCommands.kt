package me.hugo.thankmaslobby.commands

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.FishTypeRegistry
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.bukkit.annotation.CommandPermission

public class LobbyCommands(private val instance: ThankmasLobby) : TranslatedComponent {

    private val fishRegistry: FishTypeRegistry by inject()

    @Command("fishes")
    private fun openFishesMenu(sender: Player) {
        fishRegistry.fishTypesMenu.open(sender)
    }

    @Command("fishbag", "capturedfishes")
    private fun openFishBag(sender: Player) {
        instance.playerManager.getPlayerData(sender.uniqueId).fishBag.open(sender)
    }

    @Command("reloadtranslations")
    @CommandPermission("thankmaslobby.reloads")
    private fun reloadLang(sender: Player, @Optional type: TranslationType = TranslationType.LOCAL) {
        when (type) {
            TranslationType.LOCAL -> miniPhrase.translationRegistry.reload()
            TranslationType.GLOBAL -> ThankmasPlugin.instance().globalTranslations.translationRegistry.reload()
        }

        val scoreboardManager: LobbyScoreboardManager by inject()
        scoreboardManager.initialize()

        instance.playerManager.getPlayerData(sender.uniqueId).reloadTranslations(sender.locale())

        sender.sendMessage(Component.text("Reloaded messages in context $type!", NamedTextColor.GREEN))
    }

    private enum class TranslationType {
        GLOBAL, LOCAL
    }
}