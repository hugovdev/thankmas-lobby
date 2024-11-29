package me.hugo.thankmaslobby.decoration

import me.hugo.thankmas.items.model
import me.hugo.thankmas.world.AnvilWorldRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.joml.Matrix4f
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public class SummoningCircles(worldName: String) : KoinComponent {

    private val anvilWorldRegistry: AnvilWorldRegistry by inject()

    public val summoningCircles: MutableList<SummoningCircle> = mutableListOf()

    init {
        val world = Bukkit.getWorld(worldName)

        if (world != null) {
            anvilWorldRegistry.getMarkerForType(worldName, "summoning_circle").forEach { marker ->
                summoningCircles += SummoningCircle(marker.location.toLocation(world).clone().add(0.0, 1.1, 0.0))
            }

            Bukkit.getScheduler().runTaskTimer(ThankmasLobby.instance(), Runnable {
                summoningCircles.forEach {
                    it.rotateLowerCircle()
                    it.rotateHigherCircle()
                    it.moveFloatingKweebec()
                }
            }, 0L, 40L)
        }
    }

    /** Summoning circle composed of 3 entities with functions to animate them. */
    public class SummoningCircle(location: Location) {
        private val lowerCircleMatrix: Matrix4f = Matrix4f().scale(2.0f)

        private val lowerCircle: ItemDisplay =
            (location.world.spawnEntity(location, EntityType.ITEM_DISPLAY) as ItemDisplay).also {
                it.setItemStack(ItemStack(Material.PHANTOM_MEMBRANE).model("decor/summoning_circle"))
                it.setTransformationMatrix(lowerCircleMatrix)
                it.interpolationDuration = 40
            }

        public fun rotateLowerCircle() {
            lowerCircle.setTransformationMatrix(lowerCircleMatrix.rotateY(Math.toRadians(180.0).toFloat() + 30.0f))
            lowerCircle.interpolationDelay = 0
            lowerCircle.interpolationDuration = 40
        }

        private val higherCircleMatrix: Matrix4f = Matrix4f().scale(1.5f)

        private val higherCircle: ItemDisplay =
            (location.world.spawnEntity(
                location.clone().add(0.0, 0.2, 0.0),
                EntityType.ITEM_DISPLAY
            ) as ItemDisplay).also {
                it.setItemStack(ItemStack(Material.PHANTOM_MEMBRANE).model("decor/summoning_circle"))
                it.interpolationDuration = 40
            }

        public fun rotateHigherCircle() {
            higherCircle.setTransformationMatrix(higherCircleMatrix.rotateY(Math.toRadians(180.0).toFloat() - 10.0f))
            higherCircle.interpolationDelay = 0
            higherCircle.interpolationDuration = 40
        }

        private var goingUp: Boolean = true
        private val floatingKweebecMatrix: Matrix4f = Matrix4f().scale(2.0f)

        private val floatingKweebec: ItemDisplay =
            (location.world.spawnEntity(
                location.clone().add(0.0, 1.8, 0.0),
                EntityType.ITEM_DISPLAY
            ) as ItemDisplay).also {
                it.setItemStack(ItemStack(Material.PHANTOM_MEMBRANE).model("decor/kweebec_particle"))
                it.interpolationDuration = 40
                it.billboard = Display.Billboard.VERTICAL
            }

        public fun moveFloatingKweebec() {
            floatingKweebec.setTransformationMatrix(
                floatingKweebecMatrix.translate(0.0f, if (goingUp) 0.5f else -0.5f, 0.0f)
            )

            floatingKweebec.interpolationDelay = 0
            floatingKweebec.interpolationDuration = 40

            goingUp = !goingUp
        }

    }

}