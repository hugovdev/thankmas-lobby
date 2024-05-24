package me.hugo.thankmaslobby.fishing.pond

import com.google.common.collect.HashMultimap
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.config.string
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.markers.registry.MarkerRegistry
import me.hugo.thankmas.math.formatToTime
import me.hugo.thankmas.player.playSound
import me.hugo.thankmas.player.translate
import me.hugo.thankmas.region.Region
import me.hugo.thankmas.registry.AutoCompletableMapRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.player.updateBoardTags
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.FishHook
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerFishEvent
import org.koin.core.annotation.Single
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Registry of every pond in the lobby.
 */
@Single
public class PondRegistry(config: FileConfiguration, private val instance: ThankmasLobby) :
    AutoCompletableMapRegistry<Pond>(Pond::class.java), TranslatedComponent, Listener {

    private val playerManager = ThankmasLobby.instance().playerManager
    private val flyingHooks: ConcurrentMap<FishHook, Particle> = ConcurrentHashMap()
    private val pondAreas: HashMultimap<Pond, Region> = HashMultimap.create()

    init {
        val fishRegistry: FishTypeRegistry by inject()

        config.getKeys(false).forEach { pondId ->
            register(
                pondId, Pond(
                    pondId,
                    config.string("$pondId.name"),
                    config.string("$pondId.description"),
                    config.getString("$pondId.enter-message"),
                    config.getConfigurationSection("$pondId.fish-weights")?.getKeys(false)?.associate { fishId ->
                        Pair(fishRegistry.get(fishId), config.getDouble("$pondId.fish-weights.$fishId"))
                    } ?: mapOf()
                )
            )
        }

        val markerRegistry: MarkerRegistry by inject()

        // Load all pond area markers.
        markerRegistry.getMarkerForType("pond_area").forEach { marker ->
            val pondId = requireNotNull(marker.getString("pond_id"))
            { "No pond id has been specified for pond area in ${marker.location}." }

            val pond = get(pondId)

            pondAreas.get(pond).add(
                marker.toRegion(ThankmasLobby.instance().hubWorld).toTriggering(
                    onEnter = { player ->
                        player.inventory.setItem(
                            2,
                            playerManager.getPlayerData(player.uniqueId).selectedRod.value.buildRod(player)
                        )
                        pond.enterMessage?.let { player.sendTranslated(it) }
                    },
                    onLeave = { player ->
                        player.inventory.setItem(2, null)
                    })
            )
        }

        Bukkit.getScheduler().runTaskTimer(ThankmasLobby.instance(), Runnable {
            flyingHooks.forEach { (hook, particle) ->
                if (hook.isDead || !hook.isValid || hook.isOnGround || hook.isInWater || hook.hookedEntity != null) {
                    flyingHooks.remove(hook)
                    return@Runnable
                }

                val hookLocation = hook.location
                hookLocation.world.spawnParticle(particle, hookLocation, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }, 0, 1)
    }

    @EventHandler
    private fun onRodCast(event: ProjectileLaunchEvent) {
        val hook = event.entity as? FishHook ?: return
        val player = hook.shooter as? Player ?: return

        val selectedRod = instance.playerManager.getPlayerData(player.uniqueId).selectedRod.value

        // Apply rod fish and bite times!
        selectedRod.apply(hook)
        flyingHooks[hook] = selectedRod.particle
    }

    @EventHandler
    private fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player
        val pond = pondAreas.keys().firstOrNull {
            pondAreas.get(it).firstOrNull { region -> event.hook.location in region } != null
        }

        if (pond == null) {
            player.sendTranslated("fishing.pond.out_of_bounds")
            event.hook.remove()

            event.isCancelled = true
            return
        }

        if (event.state == PlayerFishEvent.State.BITE) {
            player.playSound(Sound.BLOCK_NOTE_BLOCK_PLING)
            return
        }

        val playerData = instance.playerManager.getPlayerData(player.uniqueId)

        if (event.state == PlayerFishEvent.State.FISHING) {
            playerData.lastHookShoot = System.currentTimeMillis()
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

        playerData.captureFish(caughtFish, pond.pondId)

        player.sendTranslated(caughtFish.rarity.getCaughtMessage()) {
            inserting("fish", player.translate(caughtFish.name))
            inserting("time", (System.currentTimeMillis() - playerData.lastHookShoot).formatToTime(player))
        }

        player.updateBoardTags("fishes")
    }

}