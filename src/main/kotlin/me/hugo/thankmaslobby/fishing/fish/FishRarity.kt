package me.hugo.thankmaslobby.fishing.fish

import dev.kezz.miniphrase.tag.TagResolverBuilder
import me.hugo.thankmas.lang.Translated
import net.kyori.adventure.text.Component
import java.util.Locale

/**
 * Different rarities a fish type can have.
 */
public enum class FishRarity(private val translationKey: String) : Translated {

    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    EPIC("epic"),
    LEGENDARY("legendary"),
    GODLY("godly");

    /** @returns the translation used for this rarity's tag. */
    public fun getTag(): String = "fishing.rarity.$translationKey"

    /** @returns the translation used when a fish of this rarity is caught. */
    public fun getCaughtMessage(): String = "fishing.rarity.$translationKey.caught"

    /** @returns the translation used when a fish of this rarity is caught, broadcasted globally. */
    public fun getGlobalCaughtMessage(): String = "fishing.rarity.$translationKey.caught_global"

}