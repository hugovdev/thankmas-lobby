package me.hugo.thankmaslobby.player

import me.hugo.thankmas.player.ScoreboardPlayerData
import me.hugo.thankmaslobby.fishing.CapturedFish
import java.util.UUID

public class LobbyPlayer(playerUUID: UUID) : ScoreboardPlayerData(playerUUID) {

    private val unlockedNPCs: MutableList<EasterEggNPC> = mutableListOf()
    private val capturedFishes: MutableList<CapturedFish> = mutableListOf()

    init {
        // Load from SQL
    }

}