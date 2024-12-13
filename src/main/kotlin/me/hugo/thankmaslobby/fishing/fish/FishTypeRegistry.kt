package me.hugo.thankmaslobby.fishing.fish

import kotlinx.coroutines.Runnable
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.config.enum
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.PaginatedMenu
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.registry.MapBasedRegistry
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.commands.ProfileMenuAccessor
import org.bukkit.Bukkit
import org.koin.core.annotation.Single
import org.koin.core.component.inject

/**
 * Loads fish types from a configuration file and
 * provides functions to access them.
 */
@Single
public class FishTypeRegistry : MapBasedRegistry<String, FishType>(), TranslatedComponent {

    private val profileMenuAccessor: ProfileMenuAccessor by inject()
    private val configProvider: ConfigurationProvider by inject()

    /** Menu that displays the unlocked fishes of the viewer. */
    public lateinit var fishTypesMenu: PaginatedMenu

    init {
        val config = configProvider.getOrLoad("hub/fishing/fishes.yml")

        config.getKeys(false).forEach { fishKey ->
            val fishType = FishType(
                fishKey,
                config.getString("$fishKey.name") ?: "fishing.$fishKey",
                config.enum<FishRarity>("$fishKey.rarity")
            )

            register(fishType.id, fishType)
        }

        val menuConfig = configProvider.getOrLoad("hub/menus.yml")

        // Wait until the profile menu accessor exists!
        Bukkit.getScheduler().runTaskLater(ThankmasLobby.instance(), Runnable {
            fishTypesMenu = PaginatedMenu(
                menuConfig,
                "menus.unlocked-fishes",
                lastMenu = profileMenuAccessor.profileMenu,
                miniPhrase = miniPhrase
            ).apply {
                getValues().sortedBy { it.rarity.ordinal }.forEach { fishType ->
                    addIcon(Icon { player ->
                        val playerData = ThankmasLobby.instance().playerDataManager.getPlayerData(player.uniqueId)
                        fishType.getIcon(fishType !in playerData.unlockedFish, player.locale())
                    })
                }
            }
        }, 1L)
    }
}