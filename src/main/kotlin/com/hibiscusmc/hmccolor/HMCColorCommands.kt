package com.hibiscusmc.hmccolor

import me.mattstudios.mf.annotations.*
import me.mattstudios.mf.base.CommandBase
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player

var colorConfig = HMCColorConfig()

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
        createGui().open(this)
    }

    @SubCommand("reload")
    @Permission("hmccolor.reload")
    fun Player.reloadCommand() {
        hmcColor.reloadConfig()
        colorConfig.reload()
        cachedDyeMap.clear()
        cachedDyeMap = getDyeColorList()
        this.sendMessage("${ChatColor.GREEN}Successfully reloaded the config!")
    }

}
