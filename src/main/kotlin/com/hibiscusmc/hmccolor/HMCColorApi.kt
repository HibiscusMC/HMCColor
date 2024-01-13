package com.hibiscusmc.hmccolor

import org.bukkit.entity.Player

object HMCColorApi {

    fun getOrCreateColorMenu(player: Player) = player.getOrCreateColorMenu()

    fun openColorMenu(player: Player) {
        player.getOrCreateColorMenu().open(player)
    }
}
