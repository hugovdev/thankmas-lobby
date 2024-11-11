package me.hugo.thankmaslobby.fishing.fish

import me.hugo.thankmas.lang.Translated
import net.kyori.adventure.text.format.NamedTextColor

/** Different rarities a fish type can have. */
public enum class FishRarity(private val translationKey: String, public val rarityColor: NamedTextColor) : Translated {

    COMMON("common", NamedTextColor.WHITE),
    UNCOMMON("uncommon", NamedTextColor.GREEN),
    RARE("rare", NamedTextColor.BLUE),
    EPIC("epic", NamedTextColor.DARK_PURPLE),
    LEGENDARY("legendary", NamedTextColor.GOLD),
    GODLY("godly", NamedTextColor.YELLOW);

    /** @returns the translation used for this rarity's tag. */
    public fun getTag(): String = "fishing.rarity.$translationKey"

    /** @returns the translation used when a fish of this rarity is caught. */
    public fun getCaughtMessage(): String = "fishing.rarity.$translationKey.caught"

    /** @returns the translation used when a fish of this rarity is caught, broadcasted globally. */
    public fun getGlobalCaughtMessage(): String = "fishing.rarity.$translationKey.caught_global"

}