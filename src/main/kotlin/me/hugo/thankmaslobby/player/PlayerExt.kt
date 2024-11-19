package me.hugo.thankmaslobby.player

import dev.kezz.miniphrase.MiniPhraseContext
import dev.kezz.miniphrase.audience.sendTranslated
import me.hugo.thankmas.player.translate
import org.bukkit.entity.Player

context(MiniPhraseContext)
public fun Player.isDonor(perk: String, verb: String? = null): Boolean {
    if (hasPermission("thankmas.donator")) return true
    else {
        sendTranslated("donator_required") {
            if (verb != null) inserting("verb", translate(verb))
            else parsed("verb", "")

            inserting("perk", translate(perk))
        }
        return false
    }
}