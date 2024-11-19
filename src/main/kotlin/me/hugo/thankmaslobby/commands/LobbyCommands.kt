package me.hugo.thankmaslobby.commands

import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.translate
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.database.Fishes
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.pond.Pond
import me.hugo.thankmaslobby.fishing.rod.FishingRod
import me.hugo.thankmaslobby.game.GameRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.transactions.transaction
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
    private suspend fun openGameSelector(sender: Player) {
        val gameRegistry: GameRegistry by inject()
        gameRegistry.gameSelector.open(sender)
    }

    @Command("fishbag", "capturedfishes")
    private fun openFishBag(sender: Player) {
        instance.playerDataManager.getPlayerData(sender.uniqueId).fishBag.open(sender)
    }

    @Command("unlockrod")
    @CommandPermission("thankmas.admin")
    private fun unlockRod(
        sender: Player,
        fishingRod: FishingRod,
        @Optional receiver: Player = sender,
        @Switch("save", defaultValue = false) save: Boolean = false
    ) {
        val playerData = instance.playerDataManager.getPlayerData(receiver.uniqueId)

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

    @Command("leaderboard")
    @CommandPermission("thankmas.admin")
    private fun viewLeaderboard(sender: Player, pond: Pond) {
        sender.sendMessage(Component.text("Asking the database for a leaderboard...", NamedTextColor.GREEN))

        Bukkit.getScheduler().runTaskAsynchronously(instance, Runnable {
            transaction {
                Fishes
                    .select(Fishes.whoCaught, Fishes.whoCaught.count())
                    .where { Fishes.pondId eq pond.pondId }
                    .groupBy(Fishes.whoCaught)
                    .orderBy(Fishes.whoCaught.count(), SortOrder.DESC)
                    .limit(10)
                    .forEachIndexed { index, resultRow ->
                        sender.sendMessage("${index + 1}. ${resultRow[Fishes.whoCaught]} -> ${resultRow[Fishes.whoCaught.count()]}")
                    }
            }
        })
    }
}