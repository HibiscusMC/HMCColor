package com.hibiscusmc.hmccolor

import io.th0rgal.oraxen.commands.CommandsManager
import me.mattstudios.mf.base.CommandManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val hmcColor = Bukkit.getPluginManager().getPlugin("HMCColor") as JavaPlugin
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        hmcColor.server.pluginManager.registerEvents(HMCColorListeners(), hmcColor)
        CommandManager(hmcColor).register(HMCColorCommands())
    }

    override fun onDisable() {

    }
}
