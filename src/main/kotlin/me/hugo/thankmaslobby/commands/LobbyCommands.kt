package me.hugo.thankmaslobby.commands

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.translate
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.game.GameRegistry
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Switch
import revxrsal.commands.bukkit.annotation.CommandPermission

public class LobbyCommands(private val instance: ThankmasLobby) : TranslatedComponent {

    private val fishRegistry: FishTypeRegistry by inject()

    @Command("fishes")
    private fun openFishesMenu(sender: Player) {
        fishRegistry.fishTypesMenu.open(sender)
    }

    @Command("games")
    private fun openGameSelector(sender: Player) {
        val gameRegistry: GameRegistry by inject()
        gameRegistry.gameSelector.open(sender)
    }

    @Command("fishbag", "capturedfishes")
    private fun openFishBag(sender: Player) {
        instance.playerManager.getPlayerData(sender.uniqueId).fishBag.open(sender)
    }

    @Command("reloadtranslations")
    @CommandPermission("thankmas.admin")
    private fun reloadLang(sender: Player, @Optional type: TranslationType = TranslationType.LOCAL) {
        when (type) {
            TranslationType.LOCAL -> miniPhrase.translationRegistry.reload()
            TranslationType.GLOBAL -> ThankmasPlugin.instance().globalTranslations.translationRegistry.reload()
        }

        val scoreboardManager: LobbyScoreboardManager by inject()
        scoreboardManager.initialize()

        instance.playerManager.getPlayerData(sender.uniqueId).setTranslation(sender.locale())

        sender.sendMessage(Component.text("Reloaded messages in context $type!", NamedTextColor.GREEN))
    }

    @Command("unlockrod")
    @CommandPermission("thankmas.admin")
    private fun unlockRod(
        sender: Player,
        fishingRod: FishingRod,
        @Optional receiver: Player = sender,
        @Switch("save", defaultValue = false) save: Boolean = false
    ) {
        val playerData = instance.playerManager.getPlayerData(sender.uniqueId)

        if (playerData.unlockedRods.containsKey(fishingRod)) {
            sender.sendMessage(Component.text("You already have this rod unlocked!", NamedTextColor.RED))
            return
        }

        playerData.unlockedRods[fishingRod] = FishingRod.FishingRodData(System.currentTimeMillis(), save)
        sender.sendMessage(
            Component.text("Unlocked ", NamedTextColor.GREEN)
                .append(sender.translate(fishingRod.getItemName()))
                .append(Component.text(" for ${receiver.name}" + (if (save) " and saved!" else " temporarily!")))
        )
    }

    private enum class TranslationType {
        GLOBAL, LOCAL
    }
}