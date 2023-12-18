package me.hugo.thankmaslobby.fishing.fish

import dev.kezz.miniphrase.tag.TagResolverBuilder
import me.hugo.thankmas.items.TranslatableItem
import me.hugo.thankmas.items.addLoreTranslatable
import me.hugo.thankmas.items.nameTranslatable
import me.hugo.thankmas.lang.Translated
import org.bukkit.inventory.ItemStack
import java.util.*

public class FishType(public val name: String, public val rarity: FishRarity, private val item: TranslatableItem) :
    Translated {

    /** @returns the cached item of this fish in [locale]. */
    public fun getItem(locale: Locale, tags: (TagResolverBuilder.() -> Unit)? = null): ItemStack {
        val fishItem = ItemStack(item.getBaseItem())
            .nameTranslatable(item.name, locale, tags)
            .addLoreTranslatable(rarity.getTag(), locale, tags)
            .addLoreTranslatable(item.lore, locale, tags)

        return fishItem
    }

}