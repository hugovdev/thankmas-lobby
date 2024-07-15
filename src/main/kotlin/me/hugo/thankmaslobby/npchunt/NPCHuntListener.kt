package me.hugo.thankmaslobby.npchunt

import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.entity.npc.PlayerNPCMarkerRegistry
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.playSound
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.player.LobbyPlayer
import me.hugo.thankmaslobby.player.updateBoardTags
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/** Registers when players finding NPC Hunt NPCs. */
public class NPCHuntListener(private val playerNPCRegistry: PlayerNPCMarkerRegistry<LobbyPlayer>) : Listener,
    TranslatedComponent {

    public companion object {
        /** Key used to identify NPC Hunt NPCs. */
        public const val NPC_HUNT_USE_KEY: String = "npc_hunt"
    }

    @EventHandler
    private fun onNPCRightClick(event: NPCRightClickEvent) {
        val npc = event.npc

        val clicker = event.clicker
        val playerProfile = ThankmasLobby.instance().playerManager.getPlayerData(clicker.uniqueId)

        val npcUse: String? = npc.data().get("use") as? String?
        if (npcUse != NPC_HUNT_USE_KEY) return

        val npcId: String? = npc.data().get("id") as? String?
        requireNotNull(npcId) { "Tried to find Hunt NPC without an id!" }

        val markerData = playerNPCRegistry.get(npcId).second
        val phrase = (markerData.getStringList("phrases") ?: emptyList()).randomOrNull()

        val displayName = markerData.getString("display_name")

        if (phrase != null) {
            clicker.sendTranslated("npc_hunt.talk") {
                parsed("npc_phrase", phrase)

                markerData.getKeys().forEach {
                    parsed(it, markerData.getString(it))
                }
            }

            clicker.playSound(Sound.ENTITY_VILLAGER_YES)
        }

        if (!playerProfile.hasFound(npcId)) {
            playerProfile.find(npcId)

            clicker.sendTranslated("npc_hunt.found") {
                parsed("display_name", displayName)
            }

            clicker.playSound(Sound.ENTITY_PLAYER_LEVELUP)

            clicker.updateBoardTags("npcs")
        }
    }

}