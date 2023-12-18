package me.hugo.thankmaslobby.fishing.pond

import me.hugo.thankmas.config.getStringNotNull
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.region.Region
import me.hugo.thankmas.registry.MapBasedRegistry
import me.hugo.thankmaslobby.fishing.FishTypeRegistry
import org.bukkit.configuration.file.FileConfiguration
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Registry of every pond in the lobby.
 */
@Single
public class PondRegistry(config: FileConfiguration, path: String) : MapBasedRegistry<String, Pond>(), KoinComponent {

    init {
        val fishRegistry: FishTypeRegistry by inject()

        config.getConfigurationSection(path)?.getKeys(false)?.forEach { pondId ->
            register(
                pondId, Pond(
                    pondId,
                    config.getStringNotNull("$path.$pondId.name"),
                    config.getStringNotNull("$path.$pondId.description"),
                    config.getString("$path.$pondId.enter-message"),
                    TranslatableItem(config, "$path.$pondId.fishing-rod"),
                    Region(config, "$path.$pondId.region"),
                    config.getConfigurationSection("$path.$pondId.fish-weights")?.getKeys(false)?.associate { fishId ->
                        Pair(fishRegistry.get(fishId), config.getDouble("$path.$pondId.fish-weights.$fishId"))
                    } ?: mapOf()
                )
            )
        }
    }

}