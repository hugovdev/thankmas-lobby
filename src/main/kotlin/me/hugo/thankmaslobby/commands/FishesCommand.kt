package me.hugo.thankmaslobby.commands

import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.FishTypeRegistry
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.DefaultFor

public class FishesCommand(private val instance: ThankmasLobby) : KoinComponent {

    private val fishRegistry: FishTypeRegistry by inject()

    @Command("fishes")
    private fun openFishesMenu(sender: Player) {
        fishRegistry.fishTypesMenu.open(sender)
    }

    @Command("fishbag", "capturedfishes")
    private fun openFishBag(sender: Player) {
        instance.playerManager.getPlayerData(sender.uniqueId).fishBag.open(sender)
    }

}