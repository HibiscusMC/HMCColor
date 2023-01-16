package com.hibiscusmc.hmccolor

import org.bukkit.configuration.ConfigurationSection

// Just to make the code abit prettier :)
data class Effects(val name: String, val color: String)
data class Colors(val baseColor: BaseColor, val subColors: Set<SubColor>)
data class BaseColor(val name: String, val color: String)
data class SubColor(val name: String, val color: String)
class HMCColorConfig {
    private var config = hmcColor.config
    val effectsEnabled = config.getBoolean("enable_effects_menu", false)
    val title = config.getString("title", "HMCColor")!!
    private val buttons = config.getConfigurationSection("buttons")
    val oraxenItem = buttons?.getString("oraxen_item", "")
    val crucibleItem = buttons?.getString("crucible_item", "")
    val itemsAdderItem = buttons?.getString("items_adder_item", "")
    val lootyItem = buttons?.getString("looty_item", "")
    val defaultItem = buttons?.getString("default_type", "LEATHER_HORSE_ARMOR")
    val customModelData = buttons?.getInt("custom_model_data", 0)
    private val blacklist = config.getConfigurationSection("blacklist")
    val blacklistedOraxen: List<String> = blacklist?.getStringList("oraxen_items") ?: emptyList()
    val blacklistedCrucible: List<String> = blacklist?.getStringList("crucible_items") ?: emptyList()
    val blacklistedItemsAdder: List<String> = blacklist?.getStringList("itemsadder_items") ?: emptyList()
    val blacklistedLooty: List<String> = blacklist?.getStringList("looty_items") ?: emptyList()
    val blacklistedTypes: List<String> = blacklist?.getStringList("types") ?: emptyList()
    val colors = config.getConfigurationSection("colors")?.getColors() ?: emptySet()
    val effects = if (effectsEnabled) config.getConfigurationSection("effects")?.getEffectsColors() ?: emptySet() else emptySet()

    fun reload() {
        hmcColor.reloadConfig()
        colorConfig.config = hmcColor.config
    }

    private fun ConfigurationSection.getEffectsColors(): Set<Effects> {
        return this.getKeys(false).map {
            this.getConfigurationSection(it).let { c ->
                Effects(c?.getString("name", "") ?: "", c?.getString("color") ?: "#FFFFFF")
            }
        }.toSet()
    }

    private fun ConfigurationSection.getColors(): Set<Colors> {
        val colors = mutableSetOf<Colors>()
        this.getKeys(false).forEach { colorKey ->
            val subColorList = mutableListOf<SubColor>()
            val c = this.getConfigurationSection(colorKey)!!
            val s = c.getConfigurationSection("subColors")!!

            s.getKeys(false).forEach { subColor ->
                val sub = s.getConfigurationSection(subColor)!!
                subColorList.add(SubColor(sub.getString("name", "") ?: "", sub.getString("color") ?: "#FFFFFF"))
            }

            colors.add(
                Colors(
                    BaseColor(c.getString("name", "") ?: "", c.getString("baseColor") ?: "#FFFFFF"),
                    subColorList.toSet()
                )
            )
        }
        return colors
    }
}


