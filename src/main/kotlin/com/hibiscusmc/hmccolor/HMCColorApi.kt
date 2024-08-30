package com.hibiscusmc.hmccolor

import org.bukkit.entity.Player

class HMCColorApi {

    companion object {
        @JvmStatic
        fun createColorMenu(player: Player) = hmcColor.helpers.createColorMenus(player)

        @JvmStatic
        fun openColorMenu(player: Player) {
            createColorMenu(player).open(player)
        }
    }
}
