package com.hibiscusmc.hmccolor

import me.mattstudios.mf.base.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val hmcColor = Bukkit.getPluginManager().getPlugin("HMCColor") as HMCColor
val mm = MiniMessage.miniMessage()
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        CommandManager(hmcColor).register(HMCColorCommands())
    }

    override fun onDisable() {

    }
}
