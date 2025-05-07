package me.hugo.thankmaslobby.music

import me.hugo.thankmas.ThankmasPlugin
import me.hugo.thankmas.music.MusicManager
import me.hugo.thankmas.region.types.MusicalRegion
import me.hugo.thankmaslobby.ThankmasLobby
import org.bukkit.entity.Player
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

/** Special music manager that plays a song depending on where the listener is! */
@Single
public class LobbyMusic : MusicManager() {

    public companion object {
        /** The default track that plays when a person is inside no specific region. */
        public const val DEFAULT_TRACK: String = "music.kweebec_park"
    }

    /** For each track id, which track with defined duration should it play. */
    private val tracksById: Map<String, MusicTrack> = mapOf(
        Pair("music.neon_city", MusicTrack("music.neon_city", 96.seconds)),
        Pair("music.medieval_town", MusicTrack("music.medieval_town", 104.seconds)),
        Pair("music.kweebec_park", MusicTrack("music.kweebec_park", 56.seconds)),
        Pair("music.alchemy_district", MusicTrack("music.alchemy_district", 78.seconds))
    )

    /**
     * When the current song ends, this lobby music resolver looks for the last
     * musical region entered by the player and plays its music track.
     */
    override val musicResolver: (listener: Player) -> MusicTrack? = {
        val playerData = ThankmasPlugin.instance<ThankmasLobby>().playerDataManager.getPlayerDataOrNull(it.uniqueId)
        val lastMusicalRegion = playerData?.lastRegion { it is MusicalRegion } as MusicalRegion?

        // Get the track that belongs to this player depending on their last musical region,
        // if no region is found use the default!
        tracksById.getValue(lastMusicalRegion?.songId ?: DEFAULT_TRACK)
    }

    public fun playDefaultTrack(listener: Player) {
        playTrack(tracksById.getValue(DEFAULT_TRACK), listener)
    }

}