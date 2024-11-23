package me.hugo.thankmaslobby.fishing.pond

import dev.kezz.miniphrase.audience.sendTranslatedIfPresent
import me.hugo.thankmas.markers.Marker
import me.hugo.thankmas.player.playSound
import me.hugo.thankmas.region.WorldRegion
import me.hugo.thankmaslobby.ThankmasLobby
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.World
import org.bukkit.entity.Player
import java.time.Duration

/** Visuals and rod-giving when entering ponds. */
public class PondRegion(private val pond: Pond, marker: Marker, world: World) : WorldRegion(marker, world) {

    private val instance = ThankmasLobby.instance()

    override fun onEnter(player: Player) {
        player.inventory.setItem(
            3,
            instance.playerDataManager.getPlayerData(player.uniqueId).selectedRod.value.buildRod(player)
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
    }

    override fun onLeave(player: Player) {
        player.inventory.setItem(3, null)
    }

}