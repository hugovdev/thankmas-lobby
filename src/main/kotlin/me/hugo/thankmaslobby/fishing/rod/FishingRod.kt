package me.hugo.thankmaslobby.fishing.rod

import me.hugo.thankmas.config.enum
import me.hugo.thankmas.config.string
import me.hugo.thankmas.items.*
import me.hugo.thankmas.lang.Translated
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.FishHook
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.text.NumberFormat
import java.util.*

/** Custom fishing rod with custom times, particles and name. */
public class FishingRod(
    public val id: String,
    public val particle: Particle?,
    public val tier: Int,
    maxFishTime: Double,
    minFishTime: Double,
    maxBiteTime: Double,
    minBiteTime: Double
) : Translated {

    private val maxFishTime: Int = (maxFishTime * 20).toInt()
    private val minFishTime: Int = (minFishTime * 20).toInt()
    private val maxBiteTime: Int = (maxBiteTime * 20).toInt()
    private val minBiteTime: Int = (minBiteTime * 20).toInt()

    /**
     * Loads a fishing rod from [config] in path [path].
     */
    public constructor(config: FileConfiguration, path: String) : this(
        config.string("$path.id"),
        config.enum<Particle>("$path.particle"),
        config.getInt("$path.tier"),
        config.getDouble("$path.max-fish-time"),
        config.getDouble("$path.min-fish-time"),
        config.getDouble("$path.max-bite-time"),
        config.getDouble("$path.min-bite-time"),
    )

    public val name: String = "fishing_rods.$id.name"

    private val item: TranslatableItem = TranslatableItem(
        material = Material.FISHING_ROD,
        customModelData = tier,
        name = "fishing_rods.$id.item.name",
        lore = "fishing_rods.$id.item.lore",
        unbreakable = true,
    )

    private val icon: TranslatableItem = TranslatableItem(
        model = "rods/${id}_icon",
        name = "fishing_rods.$id.item.name",
        lore = "fishing_rods.$id.item.lore",
    )

    public companion object {
        /** Key used to identify which items stacks are fishing rods and their ids. */
        public val FISHING_ROD_ID: NamespacedKey = NamespacedKey("thankmas", "fishing_rod_id")
    }

    init {
        // Add the fishing rod id to the translatable base item!
        item.editBaseItem {
            it.setKeyedData(FISHING_ROD_ID, PersistentDataType.STRING, this.id)
            it.unbreakable(true)
            it.flags(ItemFlag.HIDE_UNBREAKABLE)
        }
    }

    /** Adds the rod stats to the lore of this item stack. */
    private fun ItemStack.addStats(locale: Locale): ItemStack {
        val decimalFormat = NumberFormat.getNumberInstance(locale)

        return this.addLoreTranslatable("fishing.fishing_rods.stats", locale) {
            parsed("min_fish_time", decimalFormat.format(minFishTime / 20.0))
            parsed("max_fish_time", decimalFormat.format(maxFishTime / 20.0))
            parsed("min_bite_time", decimalFormat.format(minBiteTime / 20.0))
            parsed("max_bite_time", decimalFormat.format(maxBiteTime / 20.0))
        }
    }

    /** Builds the rod for [player] with/without quest text! */
    public fun buildRod(player: Player, locale: Locale? = null, includeQuestText: Boolean = true): ItemStack {
        val finalLocale = locale ?: player.locale()

        return item.buildItem(finalLocale)
            .addStats(finalLocale)
            .addLoreTranslatableIf("fishing.fishing_rods.upgrade", finalLocale) { includeQuestText }
            .addLoreTranslatable("fishing.fishing_rods.click_to_cast", finalLocale)
    }

    /** Builds an icon to select this rod! */
    public fun buildIcon(
        player: Player,
        locale: Locale? = null,
        blocked: Boolean,
        selected: Boolean = false
    ): ItemStack {
        val finalLocale = locale ?: player.locale()

        return icon.buildItem(finalLocale)
            .addStats(finalLocale)
            .selectedEffect(selected)
            .addLoreTranslatable(
                if (blocked) "fishing.fishing_rods.blocked" else {
                    if (selected) "fishing.fishing_rods.selected"
                    else "fishing.fishing_rods.click_to_selected"
                },
                finalLocale
            )
    }

    /** @returns the item name translation key. */
    public fun getItemName(): String {
        return item.nameNotNull
    }

    /**
     * Applies the effects of this fishing rod to [hook].
     */
    public fun apply(hook: FishHook) {
        hook.apply {
            setWaitTime(minFishTime, maxFishTime)
            setLureTime(minBiteTime, maxBiteTime)

            resetFishingState() // Necessary since 1.21+
        }
    }

    public data class FishingRodData(public val unlockTime: Long, public val thisSession: Boolean = true)
}