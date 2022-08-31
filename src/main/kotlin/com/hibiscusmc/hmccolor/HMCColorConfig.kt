package com.hibiscusmc.hmccolor

import org.bukkit.configuration.ConfigurationSection

// Just to make the code abit prettier :)
data class Colors(val baseColor: String, val subColors: Set<String>)
class HMCColorConfig {
    private val config = hmcColor.config
    val title = config.getString("title", "HMCColor")!!
    private val buttons = config.getConfigurationSection("buttons")
    val oraxenItem = buttons?.getString("oraxen_item", "")
    val itemsAdderItem = buttons?.getString("items_adder_item", "")
    val defaultItem = buttons?.getString("default_type", "LEATHER_HORSE_ARMOR")
    val customModelData = buttons?.getInt("custom_model_data", 0)
    private val blacklist = config.getConfigurationSection("blacklist")
    val blacklistedOraxen: List<String> = blacklist?.getStringList("oraxen_items") ?: emptyList()
    val blacklistedItemsAdder: List<String> = blacklist?.getStringList("itemsadder_items") ?: emptyList()
    val blacklistedTypes: List<String> = blacklist?.getStringList("types") ?: emptyList()
    val colors = config.getConfigurationSection("colors")!!.getColors()

    private fun ConfigurationSection.getColors(): MutableMap<String, Colors> {
        val colors = mutableMapOf<String, Colors>()
        this.getKeys(false).forEach { colorKey ->
            val c = this.getConfigurationSection(colorKey)!!
            colors[colorKey] = Colors(c.getString("baseColor")!!, c.getStringList("subColors").toSet())
        }
        return colors
    }
}


