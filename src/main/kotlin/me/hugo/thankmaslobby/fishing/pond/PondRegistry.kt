package me.hugo.thankmaslobby.fishing.pond

import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.config.string
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.math.formatToTime
import me.hugo.thankmas.player.playSound
import me.hugo.thankmas.player.translate
import me.hugo.thankmas.player.updateBoardTags
import me.hugo.thankmas.registry.AutoCompletableMapRegistry
import me.hugo.thankmas.world.registry.AnvilWorldRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import net.kyori.adventure.text.Component
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** Registry of every pond in the lobby. */
@Single
public class PondRegistry(config: FileConfiguration, private val instance: ThankmasLobby) :
    AutoCompletableMapRegistry<Pond>(Pond::class.java), TranslatedComponent, Listener {

    private val flyingHooks: ConcurrentMap<FishHook, Particle> = ConcurrentHashMap()
    private val pondAreas: MutableMap<Pond, PondRegion> = mutableMapOf()

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

        val anvilWorldRegistry: AnvilWorldRegistry by inject()

        // Load all pond area markers.
        anvilWorldRegistry.getMarkerForType(instance.hubWorld.name, "pond_area").forEach { marker ->
            val pondId = requireNotNull(marker.getString("pond_id"))
            { "No pond id has been specified for pond area in ${marker.location}." }

            val pond = get(pondId)
            pondAreas[pond] = PondRegion(pond, marker, ThankmasLobby.instance().hubWorld).also { it.register() }
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

        val selectedRod = instance.playerDataManager.getPlayerData(player.uniqueId).selectedRod.value

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

        val playerData = instance.playerDataManager.getPlayerData(player.uniqueId)

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
            itemStack = caughtFish.getIcon(false, player.locale())
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