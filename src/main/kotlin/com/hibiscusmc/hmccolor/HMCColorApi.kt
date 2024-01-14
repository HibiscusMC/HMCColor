package com.hibiscusmc.hmccolor

import org.bukkit.entity.Player

class HMCColorApi {

    companion object {
        @JvmStatic
        fun createColorMenu(player: Player) = player.createColorMenu()

        @JvmStatic
        fun openColorMenu(player: Player) {
            player.createColorMenu().open(player)
        }
    }
}
