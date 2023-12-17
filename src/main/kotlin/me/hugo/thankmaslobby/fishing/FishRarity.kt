package me.hugo.thankmaslobby.fishing

import me.hugo.thankmas.lang.Translated
import net.kyori.adventure.text.Component
import java.util.Locale

/**
 * Different rarities a fish type can have.
 */
public enum class FishRarity(public val translationKey: String) : Translated {

    COMMON("fishing.rarity.common"),
    UNCOMMON("fishing.rarity.uncommon"),
    RARE("fishing.rarity.rare"),
    EPIC("fishing.rarity.epic"),
    LEGENDARY("fishing.rarity.legendary"),
    GODLY("fishing.rarity.godly");

    /** @returns the component used for this rarity. */
    public fun getText(locale: Locale): Component = miniPhrase.translate(translationKey, locale)

}