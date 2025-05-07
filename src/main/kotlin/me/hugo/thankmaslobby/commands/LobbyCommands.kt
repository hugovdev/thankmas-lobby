package me.hugo.thankmaslobby.commands

import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.game.GameRegistry
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command
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
}