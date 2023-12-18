package me.hugo.thankmaslobby.commands

import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.ConfiguredMenu
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.Menu
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.playSound
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.koin.core.annotation.Single
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command

/**
 * Creates profile menus and sub-menus, also provides
 * a command to open the main menu.
 */
@Single
public class ProfileMenuAccessor(private val instance: ThankmasLobby) : TranslatedComponent {

    private val configProvider: ConfigurationProvider by inject()

    private val menusConfig = configProvider.getOrLoad("menus")

    public val profileMenu: Menu = ConfiguredMenu(menusConfig, "menus.profile")

    public val fishingMenu: PaginatedMenu =
        ConfigurablePaginatedMenu(menusConfig, "menus.fishing", profileMenu).apply {
            val fishBagItem = TranslatableItem(menusConfig, "menus.fishing.icons.fish-bag")

            setIcon(12, 0, Icon({ context, _ ->
                val clicker = context.clicker
                val playerData = instance.playerManager.getPlayerData(clicker.uniqueId)

                if (playerData.fishAmount() == 0) {
                    clicker.sendTranslated("menu.fishing.no_fishes")
                    return@Icon
                }

                playerData.fishBag.open(clicker)
                clicker.playSound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON)
            }) {
                fishBagItem.buildItem(it.locale())
            })

            val fishCollectionItem = TranslatableItem(menusConfig, "menus.fishing.icons.fish-collection")
            setIcon(14, 0, Icon {
                fishCollectionItem.buildItem(it.locale())
            })
        }

    private val fishRegistry: FishTypeRegistry by inject()

    init {
        val fishingItem = TranslatableItem(menusConfig, "menus.profile.icons.fish-bag")

        profileMenu.setIcon(11, Icon({ context, _ ->
            val clicker = context.clicker

            fishingMenu.open(clicker)
            clicker.playSound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON)
        }) {
            val playerData = instance.playerManager.getPlayerData(it.uniqueId)

            fishingItem.buildItem(it.locale()) {
                parsed("fishes", playerData.uniqueFishTypes())
                parsed("total_fishes", fishRegistry.size())
            }
        })

        val profileItem = TranslatableItem(menusConfig, "menus.profile.icons.profile")
        profileMenu.setIcon(13, Icon {
            val playerData = instance.playerManager.getPlayerData(it.uniqueId)

            profileItem.buildItem(it.locale()) {
                parsed("fishes", playerData.uniqueFishTypes())
                parsed("total_fishes", fishRegistry.size())
                parsed("npcs", 0)
                parsed("total_npcs", 0)
                parsed("rank", "None")
                parsed("thankmas_level", "PRO")
            }
        })

        val npcItem = TranslatableItem(menusConfig, "menus.profile.icons.npc-collector")
        profileMenu.setIcon(15, Icon {
            val playerData = instance.playerManager.getPlayerData(it.uniqueId)

            npcItem.buildItem(it.locale()) {
                parsed("npcs", 0)
                parsed("total_npcs", 0)
            }
        })
    }

    @Command("profile")
    private fun openProfileMenu(sender: Player) {
        profileMenu.open(sender)
    }
}