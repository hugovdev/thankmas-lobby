package me.hugo.thankmaslobby.listener

import com.destroystokyo.paper.MaterialSetTag
import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import org.bukkit.ExplosionResult
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

/** Protects the lobby from block changes, damage and environmental changes. */
public class PlayerLobbyProtection : Listener {

    /** Blocks that can be right-clicked. */
    private val interactableBlocks: MaterialSetTag = MaterialSetTag(
        NamespacedKey("thankmas", "lobby_interactable_blocks")
    ).add(MaterialTags.TRAPDOORS, MaterialTags.DOORS)

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
    private fun onHangingBreak(event: HangingBreakEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemFrameItem(event: PlayerItemFrameChangeEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockPhysics(event: BlockPhysicsEvent) {
        // Allow doors to update their neighbouring door piece!
        if (interactableBlocks.isTagged(event.changedBlockData.material)) return

        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockInteraction(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return

        if (event.player.gameMode == GameMode.CREATIVE && event.player.hasPermission("thankmaslobby.mapchange")) return
        if (interactableBlocks.isTagged(clickedBlock)) return

        event.setUseInteractedBlock(Event.Result.DENY)
    }

    @EventHandler
    private fun onEntityTriggerBlock(event: EntityChangeBlockEvent) {
        val block = event.block

        if (block.type == Material.BIG_DRIPLEAF) {
            event.isCancelled = true
        }
    }

    @EventHandler
    private fun onEntityTriggerBlock(event: EntityExplodeEvent) {
        // Don't let wind charges interact with blocks!
        if(event.explosionResult == ExplosionResult.TRIGGER_BLOCK) {
            event.blockList().clear()
            return
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