package com.hibiscusmc.hmccolor

import me.mattstudios.mf.annotations.*
import me.mattstudios.mf.base.CommandBase
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

var colorConfig = HMCColorConfig()

@Command("hmccolor")
class HMCColorCommands : CommandBase() {

    val CONSOLE_ERROR_MSG = Component.text("This command can only be ran by players!").color(NamedTextColor.RED);

    @Default
    @Permission("hmccolor.command")
    fun CommandSender.defaultCommand() {
        this.colorCommand()
    }

    @SubCommand("color")
    @Alias("dye")
    fun CommandSender.colorCommand() {
        (this as? Player)?.let { createGui().open(it) }
            ?: Adventure.AUDIENCE.console().sendMessage(CONSOLE_ERROR_MSG)
    }

    @SubCommand("reload")
    @Permission("hmccolor.reload")
    fun CommandSender.reloadCommand() {
        cachedDyeMap.clear()
        cachedEffectSet.clear()
        colorConfig.reload()
        cachedDyeMap = getDyeColorList()
        cachedEffectSet = getEffectList()
        Adventure.AUDIENCE.sender(this).sendMessage(Component.text("Successfully reloaded the config!").color(NamedTextColor.GREEN))
    }

}
