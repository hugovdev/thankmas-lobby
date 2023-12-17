package me.hugo.thankmaslobby.fishing

import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.lang.TranslatedComponent
import org.bukkit.configuration.file.FileConfiguration
import org.koin.core.annotation.Single
import org.koin.core.component.inject

/**
 * Loads fish types from a configuration file and
 * provides functions to access them.
 */
@Single
public class FishTypeRegistry(config: FileConfiguration) : TranslatedComponent {

    private val configProvider: ConfigurationProvider by inject()

    /** Storage of every fish type loaded form config! */
    private val fishTypes: Map<String, FishType> = config.getConfigurationSection("fish-types")?.getKeys(false)
        ?.associateWith { fishKey ->
            FishType(
                config.getString("fish-types.$fishKey.name") ?: fishKey,
                FishRarity.valueOf((config.getString("fish-types.$fishKey.rarity") ?: "common").uppercase()),
                TranslatableItem(config, "fish-types.$fishKey.item")
            )
        } ?: mapOf()

    /** Menu that displays the unlocked fishes of the viewer. */
    public val fishTypesMenu: PaginatedMenu =
        ConfigurablePaginatedMenu(configProvider.getOrLoad("menus"), "menus.unlocked-fishes").apply {
            getFishTypes().forEach { fishType ->
                repeat(5) {
                    addIcon(Icon { player ->
                        fishType.getItem(player.locale())
                    })
                }
            }
        }

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