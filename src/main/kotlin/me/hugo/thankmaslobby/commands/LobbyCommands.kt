package me.hugo.thankmaslobby.commands

import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.translate
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.game.GameRegistry
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

    @Command("games")
    private suspend fun openGameSelector(sender: Player) {
        val gameRegistry: GameRegistry by inject()
        gameRegistry.gameSelector.open(sender)
    }

    @Command("getcoins")
    @CommandPermission("thankmas.superadmin")
    private fun unlockRod(
        sender: Player,
        coins: Int
    ) {
        val playerData = instance.playerDataManager.getPlayerData(sender.uniqueId)

        playerData.currency += coins

        sender.sendMessage(Component.text("Given $coins to ${sender.name}"))
    }

    @Command("unlockrod")
    @CommandPermission("thankmas.admin")
    private fun unlockRod(
        sender: Player,
        fishingRod: FishingRod,
        @Optional receiver: Player = sender
    ) {
        val playerData = instance.playerDataManager.getPlayerData(receiver.uniqueId)

        if (playerData.unlockedRods.contains(fishingRod)) {
            sender.sendMessage(Component.text("You already have this rod unlocked!", NamedTextColor.RED))
            return
        }

        playerData.unlockedRods += fishingRod
        sender.sendMessage(
            Component.text("Unlocked ", NamedTextColor.GREEN)
                .append(sender.translate(fishingRod.getItemName()))
                .append(Component.text(" for ${receiver.name} temporarily!"))
        )
    }
}