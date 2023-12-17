package me.hugo.thankmaslobby.fishing

import me.hugo.thankmas.items.load
import me.hugo.thankmas.lang.Translated
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack

public class FishTypeRegistry(config: FileConfiguration) : Translated {

    /** Storage of every fish type loaded form config! */
    private val fishTypes: Map<String, FishType> = config.getConfigurationSection("fish-types")?.getKeys(false)
        ?.associateWith { fishKey ->
            FishType(
                config.getString("fish-types.$fishKey.name", fishKey)!!,
                FishRarity.valueOf(config.getString("fish-types.$fishKey.rarity", "common")!!.uppercase()),
                ItemStack::class.load(config, "fish-types.$fishKey.item")
            )
        } ?: mapOf()

    /** @returns the fish type with the id that matches [id], can be null. */
    public fun getFishTypeOrNull(id: String): FishType? = fishTypes[id]

    /** @returns the fish type with the id that matches [id]. */
    public fun getFishType(id: String): FishType {
        val fishType = getFishTypeOrNull(id)
        requireNotNull(fishType) { "Tried to get fish type with id $id, but returned null." }

        return fishType
    }

    /** @returns all the registered fish types. */
    public fun getFishTypes(): Collection<FishType> {
        return fishTypes.values
    }

}