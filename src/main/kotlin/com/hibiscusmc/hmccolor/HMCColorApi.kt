package com.hibiscusmc.hmccolor

import com.hibiscusmc.hmccolor.extensions.Version
import com.mineinabyss.idofront.items.asColorable
import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class HMCColorApi {

    companion object {
        @JvmStatic
        fun createColorMenu(player: Player) = hmcColor.helpers.createColorMenus(player)

        @JvmStatic
        fun openColorMenu(player: Player) {
            createColorMenu(player).open(player)
        }

        @JvmStatic
        fun getItemColor(itemStack: ItemStack): Color? {
            return when {
                Version.atleast("1.21.4") -> itemStack.asColorable()?.color
                else -> itemStack.itemMeta?.asColorable()?.color
            }
        }
    }
}
