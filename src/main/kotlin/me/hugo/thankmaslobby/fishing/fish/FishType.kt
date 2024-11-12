package me.hugo.thankmaslobby.fishing.fish

import dev.kezz.miniphrase.tag.TagResolverBuilder
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.items.addLoreTranslatable
import me.hugo.thankmas.items.nameTranslatable
import me.hugo.thankmas.lang.Translated
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

/** Type of fish that can be caught. */
public class FishType(
    public val id: String,
    public val name: String,
    public val rarity: FishRarity,
) : Translated {

    public val item: TranslatableItem = TranslatableItem(
        material = Material.COD,
        model = "fish/$id",
        name = "fishing.$id.item.name",
        lore = "fishing.$id.item.lore"
    )

    /** @returns the cached item of this fish in [locale]. */
    public fun getItem(locale: Locale, tags: (TagResolverBuilder.() -> Unit)? = null): ItemStack {
        val fishItem = ItemStack(item.getBaseItem())
            .nameTranslatable(item.nameNotNull, locale, tags)
            .addLoreTranslatable(rarity.getTag(), locale, tags)
            .addLoreTranslatable(item.loreNotNull, locale, tags)

        return fishItem
    }

}