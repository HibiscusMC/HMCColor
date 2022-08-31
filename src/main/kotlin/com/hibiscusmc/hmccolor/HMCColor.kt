package com.hibiscusmc.hmccolor

import dev.triumphteam.gui.guis.GuiItem
import me.mattstudios.mf.base.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val hmcColor = Bukkit.getPluginManager().getPlugin("HMCColor") as HMCColor
val mm = MiniMessage.miniMessage()
var cachedDyeMap = mutableMapOf<GuiItem, MutableList<GuiItem>>()
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        cachedDyeMap = getDyeColorList()

        CommandManager(hmcColor).register(HMCColorCommands())
    }

    override fun onDisable() {

    }
}
