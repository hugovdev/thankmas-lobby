package me.hugo.thankmaslobby.npchunt

import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.cosmetics.CosmeticsRegistry
import me.hugo.thankmas.entity.npc.PlayerNPCMarkerRegistry
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.playSound
import me.hugo.thankmas.player.updateBoardTags
import me.hugo.thankmaslobby.ThankmasLobby
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.inject

/** Registers when players finding NPC Hunt NPCs. */
public class NPCHuntListener(private val playerNPCRegistry: PlayerNPCMarkerRegistry) : Listener,
    TranslatedComponent {

    public companion object {
        /** Key used to identify NPC Hunt NPCs. */
        public const val NPC_HUNT_USE_KEY: String = "npc_hunt"
    }

    private val cosmeticsRegistry: CosmeticsRegistry by inject()

    @EventHandler
    private fun onNPCRightClick(event: NPCRightClickEvent) {
        val npc = event.npc

        val clicker = event.clicker
        val playerProfile = ThankmasLobby.instance().playerDataManager.getPlayerData(clicker.uniqueId)

        val npcUse: String? = npc.data().get("use") as? String?
        if (npcUse != NPC_HUNT_USE_KEY) return

        val npcId: String? = npc.data().get("id") as? String?
        requireNotNull(npcId) { "Tried to find Hunt NPC without an id!" }

        val markerData = playerNPCRegistry.get(npcId).marker

        // Unlock the NPC if not already unlocked!
        if (!playerProfile.hasFound(npcId)) {
            val displayName = markerData.getString("display_name")

            playerProfile.find(npcId)

            clicker.sendTranslated("npc_hunt.found") {
                parsed("display_name", displayName)
            }

            if(playerProfile.foundNPCs().size == playerNPCRegistry.getValues()
                    .filter { it.marker.getString("use") == "npc_hunt" }.size) {
                playerProfile.acquireCosmetic(cosmeticsRegistry.get("pink_kweebec")) {
                    clicker.sendTranslated("npc_hunt.found_all")
                }
            }

            playerProfile.currency += 15
            clicker.playSound(Sound.ENTITY_PLAYER_LEVELUP)
            clicker.updateBoardTags("npcs")
        }

        val phrase = (markerData.getStringList("phrases") ?: emptyList()).randomOrNull()

        // Make the NPC yap
        if (phrase != null) {
            clicker.sendTranslated("npc_hunt.talk") {
                parsed("npc_phrase", phrase)

                markerData.getKeys().forEach {
                    parsed(it, markerData.getString(it))
                }
            }

            clicker.playSound(Sound.ENTITY_VILLAGER_YES)
        }
    }

}