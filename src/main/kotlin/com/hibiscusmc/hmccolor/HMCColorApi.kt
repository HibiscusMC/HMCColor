package com.hibiscusmc.hmccolor

import org.bukkit.entity.Player

object HMCColorApi {

    fun colorMenu() = createGui()

    fun openColorMenu(player: Player) {
        colorMenu().open(player)
    }
}
