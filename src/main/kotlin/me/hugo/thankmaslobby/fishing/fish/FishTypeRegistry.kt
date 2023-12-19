package me.hugo.thankmaslobby.fishing.fish

import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.config.string
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.registry.MapBasedRegistry
import org.koin.core.annotation.Single
import org.koin.core.component.inject

/**
 * Loads fish types from a configuration file and
 * provides functions to access them.
 */
@Single
public class FishTypeRegistry : MapBasedRegistry<String, FishType>(), TranslatedComponent {

    private val configProvider: ConfigurationProvider by inject()

    /** Menu that displays the unlocked fishes of the viewer. */
    public val fishTypesMenu: PaginatedMenu

    init {
        val config = configProvider.getOrLoad("fishes")

        config.getKeys(false).forEach { fishKey ->
            register(
                fishKey,
                FishType(
                    config.string("$fishKey.name"),
                    FishRarity.valueOf(config.string("$fishKey.rarity").uppercase()),
                    TranslatableItem(config, "$fishKey.item")
                )
            )
        }

        val menuConfig = configProvider.getOrLoad("menus")

        fishTypesMenu = ConfigurablePaginatedMenu(menuConfig, "menus.unlocked-fishes").apply {
            getValues().forEach { fishType -> addIcon(Icon { player -> fishType.getItem(player.locale()) }) }
        }
    }
}