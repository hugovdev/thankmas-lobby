package me.hugo.thankmaslobby.fishing.rod

import me.hugo.thankmas.registry.MapBasedRegistry
import org.bukkit.configuration.file.FileConfiguration
import org.koin.core.annotation.Single

/**
 * Registry of all the configured rods!
 */
@Single
public class FishingRodRegistry(config: FileConfiguration) : MapBasedRegistry<String, FishingRod>() {

    init {
        config.getKeys(false).forEach {
            val fishingRod = FishingRod(config, it)
            register(fishingRod.id, fishingRod)
        }
    }

}