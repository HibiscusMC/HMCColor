package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import io.lumine.mythiccrucible.MythicCrucible
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

val crucible by lazy { Bukkit.getPluginManager().getPlugin("MythicCrucible") as MythicCrucible }
var cachedDyeMap = mutableMapOf<GuiItem, MutableList<GuiItem>>()
var cachedEffectSet = mutableSetOf<GuiItem>()
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        createColorContext()

        cachedDyeMap = getDyeColorList()
        cachedEffectSet = getEffectList()

        HMCColorCommands()
    }

    override fun onDisable() {
        server.onlinePlayers.forEach {
            if (it.openInventory.topInventory.holder is Gui) {
                it.closeInventory()
            }
        }
    }

    fun createColorContext() {
        DI.remove<HMCColorContext>()
        val colorContext = object : HMCColorContext {
            override val plugin = this@HMCColor
            override val config: HMCColorConfig by config("config") { fromPluginPath(loadDefault = true) }
        }
        DI.add<HMCColorContext>(colorContext)
    }
}
