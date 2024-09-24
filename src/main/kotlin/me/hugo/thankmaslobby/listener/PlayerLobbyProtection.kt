package me.hugo.thankmaslobby.listener

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

/** Protects the lobby from block changes, damage and environmental changes. */
public class PlayerLobbyProtection : Listener {

    @EventHandler
    private fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.player.gameMode == GameMode.CREATIVE && event.player.hasPermission("thankmaslobby.mapchange")) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (event.player.gameMode == GameMode.CREATIVE && event.player.hasPermission("thankmaslobby.mapchange")) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onPhysicalInteraction(event: PlayerInteractEvent) {
        if (event.action != Action.PHYSICAL) return
        if (event.player.gameMode == GameMode.CREATIVE && event.player.hasPermission("thankmaslobby.mapchange")) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onEntityTriggerLeaf(event: EntityChangeBlockEvent) {
        val block = event.block

        if (block.type == Material.BIG_DRIPLEAF) {
            event.isCancelled = true
        }
    }

    @EventHandler
    private fun onEntityDamage(event: EntityDamageEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onHungerChange(event: FoodLevelChangeEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemDrop(event: PlayerDropItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemPickup(event: EntityPickupItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onExpPickup(event: PlayerPickupExperienceEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onInv(event: InventoryClickEvent) {
        if (event.whoClicked.gameMode == GameMode.CREATIVE && event.whoClicked.hasPermission("thankmaslobby.mapchange")) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemSwap(event: PlayerSwapHandItemsEvent) {
        if (event.player.gameMode == GameMode.CREATIVE && event.player.hasPermission("thankmaslobby.mapchange")) return
        event.isCancelled = true
    }

}