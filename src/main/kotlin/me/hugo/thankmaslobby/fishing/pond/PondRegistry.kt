package me.hugo.thankmaslobby.fishing.pond

import dev.kezz.miniphrase.audience.sendTranslated
import dev.kezz.miniphrase.audience.sendTranslatedIfPresent
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.config.string
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.markers.registry.MarkerRegistry
import me.hugo.thankmas.math.formatToTime
import me.hugo.thankmas.player.playSound
import me.hugo.thankmas.player.translate
import me.hugo.thankmas.region.triggering.TriggeringRegion
import me.hugo.thankmas.registry.AutoCompletableMapRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.player.updateBoardTags
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
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
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** Registry of every pond in the lobby. */
@Single
public class PondRegistry(config: FileConfiguration, private val instance: ThankmasLobby) :
    AutoCompletableMapRegistry<Pond>(Pond::class.java), TranslatedComponent, Listener {

    private val playerManager = ThankmasLobby.instance().playerManager
    private val flyingHooks: ConcurrentMap<FishHook, Particle> = ConcurrentHashMap()
    private val pondAreas: MutableMap<Pond, TriggeringRegion> = mutableMapOf()

    init {
        val fishRegistry: FishTypeRegistry by inject()

        config.getKeys(false).forEach { pondId ->
            register(
                pondId, Pond(
                    pondId,
                    Registry.SOUNDS.getOrThrow(NamespacedKey.minecraft(config.string("$pondId.enter-sound"))),
                    config.getConfigurationSection("$pondId.fish-weights")?.getKeys(false)?.associate { fishId ->
                        Pair(fishRegistry.get(fishId), config.getDouble("$pondId.fish-weights.$fishId"))
                    } ?: emptyMap()
                )
            )
        }

        val markerRegistry: MarkerRegistry by inject()

        // Load all pond area markers.
        markerRegistry.getMarkerForType("pond_area").forEach { marker ->
            val pondId = requireNotNull(marker.getString("pond_id"))
            { "No pond id has been specified for pond area in ${marker.location}." }

            val pond = get(pondId)

            pondAreas[pond] = marker.toRegion(ThankmasLobby.instance().hubWorld, pondId).toTriggering(
                onEnter = { player ->
                    player.inventory.setItem(
                        3,
                        playerManager.getPlayerData(player.uniqueId).selectedRod.value.buildRod(player)
                    )

                    player.playSound(pond.enterSound)
                    player.sendTranslatedIfPresent("fishing.pond.${pond.pondId}.enter_chat")

                    val title =
                        miniPhrase.translateOrNull("fishing.pond.${pond.pondId}.enter_title", player.locale())
                    val subtitle =
                        miniPhrase.translateOrNull("fishing.pond.${pond.pondId}.enter_subtitle", player.locale())

                    if (title != null || subtitle != null) {
                        player.showTitle(
                            Title.title(
                                title ?: Component.empty(), subtitle ?: Component.empty(),
                                Title.Times.times(
                                    Duration.ofMillis(500),
                                    Duration.ofSeconds(2),
                                    Duration.ofMillis(500)
                                )
                            )
                        )
                    }
                },
                onLeave = { it.inventory.setItem(3, null) },
                registry = instance.regionRegistry
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
        val pondRegion = pondAreas.values.firstOrNull { event.hook.location in it }

        if (pondRegion == null) {
            player.sendTranslated("fishing.pond.out_of_bounds")
            event.hook.remove()

            event.isCancelled = true
            return
        }

        val pond = get(pondRegion.id)

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

            // Create an item with a custom name so it doesn't
            // stack with other people's fish.
            itemStack = caughtFish.item.getBaseItem()
                .also { it.editMeta { it.itemName(Component.text(System.nanoTime())) } }

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