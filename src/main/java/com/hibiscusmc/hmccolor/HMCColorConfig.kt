package com.hibiscusmc.hmccolor

import org.bukkit.Color
import org.bukkit.configuration.ConfigurationSection
import java.util.ListResourceBundle

// Just to make the code abit prettier :)
data class Colors(val baseColor: String, val subColors: Set<String>)
class HMCColorConfig {
    private val config = hmcColor.config
    val title = config.getString("title", "HMCColor")!!
    val colors = config.getConfigurationSection("colors")!!.getColors()

    private fun ConfigurationSection.getColors(): MutableMap<String, Colors> {
        val colors = mutableMapOf<String, Colors>()
        this.getKeys(false).forEach { colorKey ->
            colorKey.broadcastVal("key: ")
            val c = this.getConfigurationSection(colorKey)!!
            c.getString("baseColor")?.broadcastVal("baseColor: ")
            c.getStringList("subColors").joinToString(", ").broadcastVal("subColors: ")
            colors[colorKey] = Colors(c.getString("baseColor")!!, c.getStringList("subColors").toSet())
        }
        return colors
    }
}


