package me.hugo.thankmaslobby.commands

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.FishTypeRegistry
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.DefaultFor

@Command("fishes")
public class FishesCommand : KoinComponent {

    private val fishRegistry: FishTypeRegistry by inject { parametersOf(ThankmasPlugin.instance().config) }

    @DefaultFor("fishes")
    private fun openFishesMenu(sender: Player) {
        fishRegistry.fishTypesMenu.open(sender)
    }

}