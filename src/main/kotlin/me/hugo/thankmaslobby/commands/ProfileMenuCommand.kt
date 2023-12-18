package me.hugo.thankmaslobby.commands

import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.ConfiguredMenu
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.Menu
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.FishTypeRegistry
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command

public class ProfileMenuCommand(private val instance: ThankmasLobby) : KoinComponent {

    private val configProvider: ConfigurationProvider by inject()

    private val menusConfig = configProvider.getOrLoad("menus")
    private val profileMenu: Menu = ConfiguredMenu(menusConfig, "menus.profile")

    private val fishRegistry: FishTypeRegistry by inject()

    init {
        val fishBagItem = TranslatableItem(menusConfig, "menus.profile.fish-bag-icon")

        profileMenu.setIcon(11, Icon {
            val playerData = instance.playerManager.getPlayerData(it.uniqueId)

            fishBagItem.buildItem(it.locale()) {
                parsed("fishes", playerData.uniqueFishTypes())
                parsed("total_fishes", fishRegistry.size())
            }
        })

        val profileItem = TranslatableItem(menusConfig, "menus.profile.profile-icon")
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

        val npcItem = TranslatableItem(menusConfig, "menus.profile.npc-collector-icon")
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