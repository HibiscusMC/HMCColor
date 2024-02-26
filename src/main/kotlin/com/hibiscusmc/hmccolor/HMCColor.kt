package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import dev.triumphteam.gui.guis.Gui
import io.lumine.mythiccrucible.MythicCrucible
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

val crucible by lazy { Bukkit.getPluginManager().getPlugin("MythicCrucible") as MythicCrucible }
var cachedColors = mutableMapOf<HMCColorConfig.Colors, Set<Color>>()
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        createColorContext()
        HMCColorCommands()
    }

    override fun onDisable() {
        server.onlinePlayers.filter { it.openInventory.topInventory.holder is Gui }.forEach(Player::closeInventory)
    }

    fun createColorContext() {
        DI.remove<HMCColorContext>()
        val colorContext = object : HMCColorContext {
            override val plugin = this@HMCColor
            override val config: HMCColorConfig by config("config", dataFolder.toPath(), HMCColorConfig())
        }
        DI.add<HMCColorContext>(colorContext)

        cachedColors = hmcColor.config.colors.values.associateWith { it.allColors }.toMutableMap()
    }
}
