package com.hibiscusmc.hmccolor

import org.bukkit.entity.Player

object HMCColorApi {

    fun createColorMenu(player: Player) = player.createColorMenu()

    fun openColorMenu(player: Player) {
        player.createColorMenu().open(player)
    }
}
