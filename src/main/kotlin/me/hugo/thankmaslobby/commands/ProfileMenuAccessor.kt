package me.hugo.thankmaslobby.commands

import com.destroystokyo.paper.profile.ProfileProperty
import dev.kezz.miniphrase.audience.sendTranslated
import dev.kezz.miniphrase.tag.TagResolverBuilder
import me.hugo.thankmas.config.ConfigurationProvider
import me.hugo.thankmas.gui.Icon
import me.hugo.thankmas.gui.Menu
import me.hugo.thankmas.gui.paginated.ConfigurablePaginatedMenu
import me.hugo.thankmas.gui.paginated.PaginatedMenu
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.items.loreTranslatable
import me.hugo.thankmas.items.model
import me.hugo.thankmas.items.nameTranslatable
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmas.player.playSound
import me.hugo.thankmas.player.translate
import me.hugo.thankmaslobby.ThankmasLobby
import me.hugo.thankmaslobby.fishing.fish.FishTypeRegistry
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import net.citizensnpcs.trait.SkinTrait
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.koin.core.annotation.Single
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command

/**
 * Creates profile menus and sub-menus, also provides
 * a command to open the main menu.
 */
@Single
public class ProfileMenuAccessor(private val instance: ThankmasLobby) : TranslatedComponent {

    private companion object {
        /** Skin to use when an NPC player hasn't been found yet! */
        private const val LOCKED_SKIN_TEXTURE: String =
            "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM1NDAwNjg0MywKICAicHJvZmlsZUlkIiA6ICJkYjQwYmNjNWUzMDE0ZmZjOGVlOWQxNDU5MTcyYjdhNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJhWGUxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83M2VkYzk4YTZjNzFhZmI4OTNhZTUyNjJkNjc5YzM0MTBlNzZhODhmMzkwMzgyZjMzMjM2YzNlMWUwZGZhYTY3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
        private const val LOCKED_SKIN_SIGNATURE: String =
            "OI96u/DaTO1LjrrOVLV0bDZFGpSxQGeaY6fgzk8kOoU9DhuyE++hhSSsZ1QgTsExP9ippjvWVj0CSSuHd11CmWiLCWykQKFflH4Q6CAAfELOiUZlqxvpCKinjaHA5bUhSjmJ5tKZG2zNGKyTNc+JvY7yO5vVE+iLqvHEX5ewG4gjNBeHu4m+wsdyUwVDctWwck8J22hzJS7PtmuoCN2ZRkUDIPnVxb01/SvIMsL2WIAUFmNovKsbSISLsm38LzMhWkbnrAybZcu8ZJQ2kZa/TX9LwuLKO9LXQRRUuN3Tcq6pBQRJZzOaW6zU6RiuJ9mVywDEC/s6a79FU78/U60qNkQnHISRsBk/QidwDZR1Ph2rj/2WkPItDA/MNDyW/82kwPSqq90JirvRFxvi3IxKKtJMVZ767JgDpiEDLdr5Sa0iHA2+Gc5Fk8PwasTypAd8gCk7sPsoWdNVG7n2T3RLpj2/bVXGgClQiBhndxvS5+sO46f9vbb2H8KjDrEESR2Rs21GEgj8f3X1IBu3aHuumMKxZMkbir5nj4oM64STijjoITCX1jMqiDHtFVWq74stQY3FesRcYV19ECx30WNfEMjichTPfoekMy611VnFQIKgnAOE9vJFWFKZqHTsl0g299Cf9J90MO2y6n8nLsLGYBmq/593oq7y5sJh7noflEo="
    }

    private val configProvider: ConfigurationProvider by inject()
    private val fishRegistry: FishTypeRegistry by inject()

    private val menusConfig = configProvider.getOrLoad("hub/menus.yml")

    /** Profile menu with fishing menu and npc collector menu. */
    public val profileMenu: Menu = Menu(menusConfig, "menus.profile").apply {
        val fishingItem = TranslatableItem(menusConfig, "menus.profile.icons.fish-bag")

        setIcons(Icon({ context, _ ->
            val clicker = context.clicker

            fishingMenu.open(clicker)
            clicker.playSound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON)
        }) {
            val playerData = instance.playerDataManager.getPlayerData(it.uniqueId)

            fishingItem.buildItem(it.locale()) {
                parsed("fishes", playerData.uniqueFishTypes())
                parsed("total_fishes", fishRegistry.size())
            }
        }, 0, 1, 2, 9, 10, 11, 18, 19, 20)

        val profileItem = TranslatableItem(menusConfig, "menus.profile.icons.profile")
        setIcon(13, Icon({ context, _ ->
            context.clicker.apply {
                sendTranslated("menu.profile.icon.profile.donation_message")
                playSound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON)
                closeInventory()
            }
        }) { player ->
            val playerData = instance.playerDataManager.getPlayerData(player.uniqueId)

            profileItem.buildItem(player.locale()) {
                parsed("fishes", playerData.uniqueFishTypes())
                parsed("total_fishes", fishRegistry.size())
                parsed("npcs", playerData.foundNPCs().size)
                parsed(
                    "total_npcs", instance.playerNPCRegistry.getValues()
                        .filter { it.marker.getString("use") == "npc_hunt" }.size
                )
                inserting("rank", playerData.getDecoratedRankName())
            }.also {
                if (it.type == Material.PLAYER_HEAD) {
                    it.editMeta(SkullMeta::class.java) {
                        it.setOwningPlayer(player)
                    }
                }
            }
        })

        val npcItem = TranslatableItem(menusConfig, "menus.profile.icons.npc-collector")
        setIcons(Icon({ context, _ ->
            context.clicker.apply {
                npcJournal.open(this)
                playSound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON)
            }
        }) {
            val playerData = instance.playerDataManager.getPlayerData(it.uniqueId)

            npcItem.buildItem(it.locale()) {
                parsed("npcs", playerData.foundNPCs().size)
                parsed(
                    "total_npcs", instance.playerNPCRegistry.getValues()
                        .filter { it.marker.getString("use") == "npc_hunt" }.size
                )
            }
        }, 6, 7, 8, 15, 16, 17, 24, 25, 26)
    }

    /** Fishing menu: Fish Collection or Fish Bag. */
    public val fishingMenu: PaginatedMenu =
        ConfigurablePaginatedMenu(menusConfig, "menus.fishing", profileMenu).apply {
            val fishBagItem = TranslatableItem(menusConfig, "menus.fishing.icons.fish-bag")

            setIcon(11, 0, Icon({ context, _ ->
                val clicker = context.clicker
                val playerData = instance.playerDataManager.getPlayerData(clicker.uniqueId)

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
                    val playerData = instance.playerDataManager.getPlayerData(clicker.uniqueId)

                    if (!playerData.unlockedRods.contains(rod)) {
                        clicker.sendTranslated("fishing.fishing_rods.click_when_blocked") {
                            inserting("rod", clicker.translate(rod.name))
                        }
                        clicker.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
                        return@Icon
                    }

                    if (playerData.selectedRod.value == rod) {
                        clicker.sendTranslated("fishing.fishing_rods.already_selected") {
                            inserting("rod", clicker.translate(rod.name))
                        }

                        clicker.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
                        return@Icon
                    }

                    playerData.selectedRod.value = rod
                    clicker.sendTranslated("fishing.fishing_rods.you_selected") {
                        inserting("rod", clicker.translate(rod.name))
                    }
                    clicker.playSound(Sound.BLOCK_NOTE_BLOCK_HAT)
                }) {
                    val playerData = instance.playerDataManager.getPlayerData(it.uniqueId)

                    rod.buildIcon(
                        it,
                        blocked = !playerData.unlockedRods.containsKey(rod),
                        selected = (playerData.selectedRod.value == rod)
                    )
                }.listen { instance.playerDataManager.getPlayerData(it.uniqueId).selectedRod })
            }
        }

    private val npcJournal: PaginatedMenu =
        ConfigurablePaginatedMenu(menusConfig, "menus.npc-journal", profileMenu).apply {
            instance.playerNPCRegistry.getValues().filter { it.marker.getString("use") == "npc_hunt" }
                .sortedBy { it.marker.getString("display_name") ?: it.marker.getStringList("skin")!!.first() }
                .forEach { npcData ->
                    addIcon(Icon({ context, _ ->
                        val clicker = context.clicker
                        val playerData = instance.playerDataManager.getPlayerData(clicker.uniqueId)

                        val markerData = npcData.marker
                        val unlocked = playerData.foundNPCs().contains(markerData.getString("id"))

                        if (unlocked) {
                            clicker.teleportAsync(npcData.npc.storedLocation).thenAccept {
                                clicker.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
                                clicker.sendTranslated("npc_hunt.teleported") {
                                    parsed("display_name", markerData.getString("display_name"))
                                }
                            }
                        } else clicker.playSound(Sound.BLOCK_NOTE_BLOCK_HAT)
                    }) {
                        val playerData = instance.playerDataManager.getPlayerData(it.uniqueId)

                        val markerData = npcData.marker
                        val unlocked = playerData.foundNPCs().contains(markerData.getString("id"))

                        val tags: TagResolverBuilder.() -> Unit = {
                            parsed("display_name", markerData.getString("display_name"))
                        }

                        ItemStack(Material.PLAYER_HEAD)
                            .nameTranslatable(
                                if (unlocked) "npc_hunt.icon.name.unlocked" else "npc_hunt.icon.name.locked",
                                it.locale(),
                                tags
                            )
                            .loreTranslatable(
                                if (unlocked) "npc_hunt.icon.lore.unlocked" else "npc_hunt.icon.lore.locked",
                                it.locale(),
                                tags
                            )
                            .model("gui/avatar")
                            .also { item ->
                                item.editMeta(SkullMeta::class.java) { meta ->
                                    val skinTrait: SkinTrait? = npcData.npc.getTraitNullable(SkinTrait::class.java)

                                    val signature = skinTrait?.signature
                                    val texture = skinTrait?.texture

                                    meta.playerProfile = if (!unlocked || (signature != null && texture != null)) {
                                        Bukkit.createProfile(npcData.npc.uniqueId).also { profile ->
                                            profile.setProperty(
                                                ProfileProperty(
                                                    "textures",
                                                    if (!unlocked) LOCKED_SKIN_TEXTURE else texture!!,
                                                    if (!unlocked) LOCKED_SKIN_SIGNATURE else signature
                                                )
                                            )
                                        }
                                    } else Bukkit.createProfile(npcData.marker.getStringList("skin")!!.first())
                                }
                            }
                    })
                }
        }

    @Command("profile")
    private fun openProfileMenu(sender: Player) {
        profileMenu.open(sender)
    }
}