package com.hibiscusmc.hmccolor

import me.mattstudios.mf.annotations.*
import me.mattstudios.mf.base.CommandBase
import org.bukkit.entity.Player

val colorConfig = HMCColorConfig()

@Command("hmccolor")
class HMCColorCommands : CommandBase() {

    @Default
    @Permission("hmccolor.command")
    fun Player.defaultCommand() {
        this.colorCommand()
    }

    @SubCommand("color")
    @Alias("dye")
    fun Player.colorCommand() {
        hmcColorGui?.open(this)
    }
}
