package com.hibiscusmc.hmccolor

import dev.triumphteam.gui.guis.GuiItem
import io.lumine.mythiccrucible.MythicCrucible
import me.mattstudios.mf.base.CommandManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val hmcColor by lazy { Bukkit.getPluginManager().getPlugin("HMCColor") as HMCColor }
val crucible by lazy { Bukkit.getPluginManager().getPlugin("MythicCrucible") as MythicCrucible }
var cachedDyeMap = mutableMapOf<GuiItem, MutableList<GuiItem>>()
var cachedEffectSet = mutableSetOf<GuiItem>()
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()

        cachedDyeMap = getDyeColorList()
        cachedEffectSet = getEffectList()

        CommandManager(hmcColor).register(HMCColorCommands())
    }

    override fun onDisable() {
        //cachedDyeMap.clear()
    }
}
