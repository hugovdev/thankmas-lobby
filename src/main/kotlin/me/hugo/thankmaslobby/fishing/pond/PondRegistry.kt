package me.hugo.thankmaslobby.fishing.pond

import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.config.string
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.translate
import me.hugo.thankmas.region.Region
import me.hugo.thankmas.registry.MapBasedRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.scoreboard.LobbyScoreboardManager
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.koin.core.annotation.Single
import org.koin.core.component.inject

/**
 * Registry of every pond in the lobby.
 */
@Single
public class PondRegistry(config: FileConfiguration, path: String, private val instance: ThankmasLobby) :
    MapBasedRegistry<String, Pond>(), TranslatedComponent, Listener {

    private val scoreboardManager: LobbyScoreboardManager by inject()

    init {
        val fishRegistry: FishTypeRegistry by inject()

        config.getConfigurationSection(path)?.getKeys(false)?.forEach { pondId ->
            register(
                pondId, Pond(
                    pondId,
                    config.string("$path.$pondId.name"),
                    config.string("$path.$pondId.description"),
                    config.getString("$path.$pondId.enter-message"),
                    TranslatableItem(config, "$path.$pondId.fishing-rod"),
                    Region(config, "$path.$pondId.region"),
                    config.getConfigurationSection("$path.$pondId.fish-weights")?.getKeys(false)?.associate { fishId ->
                        Pair(fishRegistry.get(fishId), config.getDouble("$path.$pondId.fish-weights.$fishId"))
                    } ?: mapOf()
                )
            )
        }
    }

    @EventHandler
    private fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player
        val pond = getValues().firstOrNull { it.region.contains(event.hook.location) }

        if (pond == null) {
            player.sendTranslated("fishing.pond.out_of_bounds")
            event.isCancelled = true
            return
        }

        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        event.expToDrop = 0

        val caughtFish = pond.catchFish()
        val item = event.caught as? Item?

        item?.apply {
            pickupDelay = Int.MAX_VALUE
            itemStack = caughtFish.getItem(miniPhrase.defaultLocale)

            Bukkit.getScheduler().runTaskLater(ThankmasPlugin.instance(), Runnable {
                remove()
            }, 15L)
        }

        val playerData = instance.playerManager.getPlayerData(player.uniqueId)
        playerData.captureFish(caughtFish, pond.pondId)

        player.sendTranslated(caughtFish.rarity.getCaughtMessage()) {
            inserting("fish", player.translate(caughtFish.name))
        }

        scoreboardManager.getTemplate("lobby").updateLinesForTag(player, "fishes")
    }

}