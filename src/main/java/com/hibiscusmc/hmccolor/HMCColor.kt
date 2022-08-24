package com.hibiscusmc.hmccolor

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val hmcColor = Bukkit.getPluginManager().getPlugin("HMCColor") as JavaPlugin
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        hmcColor.server.pluginManager.registerEvents(HMCColorListeners(), hmcColor)
        hmcColor.server.getPluginCommand("hmccolor")?.setExecutor(HMCColorCommands())
    }

    override fun onDisable() {

    }
}
