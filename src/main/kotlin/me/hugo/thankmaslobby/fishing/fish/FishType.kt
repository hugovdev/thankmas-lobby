package me.hugo.thankmaslobby.fishing.fish

import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.items.addLoreTranslatable
import me.hugo.thankmas.items.addToLore
import me.hugo.thankmas.items.name
import me.hugo.thankmas.lang.TranslatedComponent
import me.hugo.thankmaslobby.fishing.pond.PondRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.koin.core.component.inject
import java.util.*
import kotlin.math.floor

/** Type of fish that can be caught. */
public class FishType(
    public val id: String,
    public val name: String,
    public val rarity: FishRarity,
    model: String?
) : TranslatedComponent {

    private val pondRegistry: PondRegistry by inject()

    /** Item shown in menus when this fish is locked to this viewer. */
    private val lockedItem: TranslatableItem = TranslatableItem(
        material = Material.LEATHER_HORSE_ARMOR,
        model = model ?: "fish/$id",
        flags = listOf(ItemFlag.HIDE_ATTRIBUTES),
        color = 0
    )

    /** Item shown in menus when this fish is locked to this viewer. */
    private val unlockedItem: TranslatableItem = TranslatableItem(
        material = Material.PHANTOM_MEMBRANE,
        model = model ?: "fish/$id",
    )

    public fun getIcon(unlockTime: Long?, locale: Locale): ItemStack {
        val locked = unlockTime == null

        return (if (locked) lockedItem else unlockedItem).buildItem(locale).apply {
            name(
                miniPhrase.translate("fishing.$id.item.name")
                    .color(if (locked) NamedTextColor.RED else this@FishType.rarity.rarityColor)
            )

            addToLore(miniPhrase.translate(this@FishType.rarity.getTag(), locale).color(NamedTextColor.DARK_GRAY))
            addToLore(Component.empty())

            addLoreTranslatable("fishing.$id.item.lore", locale)

            addToLore(Component.empty())

            addLoreTranslatable("menu.fishing.fish_tracker.found_in", locale)

            val ponds = pondRegistry.getValues().filter { this@FishType in it.catchableFish() }
                .map {
                    miniPhrase.translate("menu.fishing.fish_tracker.found_in.element", locale) {
                        inserting("pond_name", miniPhrase.translate("fishing.pond.${it.pondId}.name", locale))
                    }
                }

            ponds.forEach { addToLore(it) }

            addToLore(Component.empty())

            addLoreTranslatable(
                if (locked) "menu.fishing.fish_tracker.locked"
                else "menu.fishing.fish_tracker.unlocked", locale
            ) {
                if (!locked) parsed("time", getTimeAgo(unlockTime!!))
            }
        }
    }

    private fun getTimeAgo(time: Long): String {
        val seconds = floor((System.currentTimeMillis() - time) / 1000.0)

        var interval = seconds / 31536000

        if (interval > 1) return "${floor(interval).toInt()} years"

        interval = seconds / 2592000
        if (interval > 1) return "${floor(interval).toInt()} months"

        interval = seconds / 86400
        if (interval > 1) return "${floor(interval).toInt()} days";

        interval = seconds / 3600
        if (interval > 1) return "${floor(interval).toInt()} hours"

        interval = seconds / 60
        if (interval > 1) return "${floor(interval).toInt()} minutes"

        return "${floor(seconds)} seconds"
    }
}