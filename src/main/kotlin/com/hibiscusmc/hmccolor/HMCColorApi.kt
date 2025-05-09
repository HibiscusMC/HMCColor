package com.hibiscusmc.hmccolor

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
            return itemStack.asColorable()?.color
        }
    }
}
