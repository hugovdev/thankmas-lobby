package me.hugo.thankmaslobby.fishing.fish

import me.hugo.thankmas.lang.Translated
import net.kyori.adventure.text.format.NamedTextColor

/** Different rarities a fish type can have. */
public enum class FishRarity(
    private val translationKey: String,
    public val rarityColor: NamedTextColor,
    public val sellPrice: Int
) : Translated {

    COMMON("common", NamedTextColor.WHITE, 10),
    UNCOMMON("uncommon", NamedTextColor.GREEN, 20),
    RARE("rare", NamedTextColor.BLUE, 35),
    EPIC("epic", NamedTextColor.DARK_PURPLE, 85),
    LEGENDARY("legendary", NamedTextColor.GOLD, 350),
    GODLY("godly", NamedTextColor.YELLOW, 1000);

    /** @returns the translation used for this rarity's tag. */
    public fun getTag(): String = "fishing.rarity.$translationKey"

    /** @returns the translation used when a fish of this rarity is caught. */
    public fun getCaughtMessage(): String = "fishing.rarity.$translationKey.caught"

    /** @returns the translation used when a fish of this rarity is caught, broadcasted globally. */
    public fun getGlobalCaughtMessage(): String = "fishing.rarity.$translationKey.caught_global"

}