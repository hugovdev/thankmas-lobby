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
import me.hugo.thankmas.player.translate
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
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
    private val fishRegistry: FishTypeRegistry by inject()

    private val menusConfig = configProvider.getOrLoad("menus")

    /** Profile menu with fishing menu and npc collector menu. */
    public val profileMenu: Menu = ConfiguredMenu(menusConfig, "menus.profile").apply {
        val fishingItem = TranslatableItem(menusConfig, "menus.profile.icons.fish-bag")

        setIcon(11, Icon({ context, _ ->
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
        setIcon(13, Icon {
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
        setIcon(15, Icon {
            val playerData = instance.playerManager.getPlayerData(it.uniqueId)

            npcItem.buildItem(it.locale()) {
                parsed("npcs", 0)
                parsed("total_npcs", 0)
            }
        })
    }

    /** Fishing menu: Fish Collection or Fish Bag. */
    public val fishingMenu: PaginatedMenu =
        ConfigurablePaginatedMenu(menusConfig, "menus.fishing", profileMenu).apply {
            val fishBagItem = TranslatableItem(menusConfig, "menus.fishing.icons.fish-bag")

            setIcon(11, 0, Icon({ context, _ ->
                val clicker = context.clicker
                val playerData = instance.playerManager.getPlayerData(clicker.uniqueId)

                if (playerData.fishAmount() == 0) {
                    clicker.sendTranslated("menu.fishing.no_fishes")
                    clicker.closeInventory()
                    return@Icon
                }

                playerData.fishBag.open(clicker)
                clicker.playSound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON)
            }) {
                fishBagItem.buildItem(it.locale())
            })

            val fishingRodSelector = TranslatableItem(menusConfig, "menus.fishing.icons.fishing-rod-selector")

            setIcon(13, 0, Icon({ context, _ ->
                val clicker = context.clicker

                rodSelector.open(clicker)
                clicker.playSound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON)
            }) {
                fishingRodSelector.buildItem(it.locale())
            })

            val fishCollectionItem = TranslatableItem(menusConfig, "menus.fishing.icons.fish-collection")
            setIcon(15, 0, Icon {
                fishCollectionItem.buildItem(it.locale())
            })
        }

    private val rodRegistry: FishingRodRegistry by inject()

    private val rodSelector: PaginatedMenu =
        ConfigurablePaginatedMenu(menusConfig, "menus.fishing-rod-selector", fishingMenu.firstPage()).apply {
            rodRegistry.getValues().sortedBy { it.tier }.forEach { rod ->
                addIcon(Icon({ context, _ ->
                    val clicker = context.clicker
                    val playerData = instance.playerManager.getPlayerData(clicker.uniqueId)

                    if (!playerData.unlockedRods.contains(rod)) {
                        clicker.sendTranslated("fishing.fishing_rods.click_when_blocked") {
                            inserting("rod", clicker.translate(rod.name))
                        }
                        clicker.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
                        return@Icon
                    }

                    if(playerData.selectedRod.value == rod) {
                        clicker.sendTranslated("fishing.fishing_rods.already_selected") {
                            inserting("rod", clicker.translate(rod.getItemName()))
                        }

                        clicker.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
                        return@Icon
                    }

                    playerData.selectedRod.value = rod
                    clicker.sendTranslated("fishing.fishing_rods.you_selected"){
                        inserting("rod", clicker.translate(rod.getItemName()))
                    }
                }) {
                    val playerData = instance.playerManager.getPlayerData(it.uniqueId)

                    rod.buildIcon(
                        it,
                        blocked = !playerData.unlockedRods.contains(rod),
                        selected = (playerData.selectedRod.value == rod)
                    )
                }.listen { instance.playerManager.getPlayerData(it.uniqueId).selectedRod })
            }
        }

    @Command("profile")
    private fun openProfileMenu(sender: Player) {
        profileMenu.open(sender)
    }
}