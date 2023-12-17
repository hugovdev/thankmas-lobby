package me.hugo.thankmaslobby.fishing

import me.hugo.thankmas.lang.Translated
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import java.util.*

public class FishType(private val name: String, private val items: Map<Locale, ItemStack>) : Translated {

    /** Returns the translated fish name in [locale]. */
    public fun getFishName(locale: Locale): Component {
        return miniPhrase.translate(name, locale)
    }

    /** @returns the cached item of this fish in [locale]. */
    public fun getItem(locale: Locale): ItemStack {
        val fishItem = items[locale] ?: items[miniPhrase.defaultLocale]
        requireNotNull(fishItem) { "Tried to get fish item for $name in ${locale.toLanguageTag()} and then ${miniPhrase.defaultLocale.toLanguageTag()} and it returned null." }

        return fishItem
    }

}