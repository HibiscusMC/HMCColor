package com.hibiscusmc.hmccolor

import com.hibiscusmc.hmccolor.Adventure.toLegacy
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.inventory.meta.FireworkEffectMeta
import org.bukkit.inventory.meta.PotionMeta

// Just to make the code abit prettier :)
data class Effects(val name: String, val color: String, val permission: String? = null)
data class Colors(val baseColor: BaseColor, val subColors: Set<SubColor>)
data class BaseColor(val name: String, val color: String)
data class BaseColorGrid(val first: IntRange, val second: IntRange, val third: IntRange)
data class SubColor(val name: String, val color: String)
class HMCColorConfig {
    private var config = hmcColor.config
    val effectsEnabled = config.getBoolean("enableEffectsMenu", false)
    val title = config.getString("title", "HMCColor")!!
    private val buttons = config.getConfigurationSection("buttons")
    val oraxenItem = buttons?.getString("oraxenItem", "")
    val crucibleItem = buttons?.getString("crucibleItem", "")
    val itemsAdderItem = buttons?.getString("itemsadderItem", "")
    val lootyItem = buttons?.getString("lootyItem", "")
    val defaultItem = buttons?.getString("defaultType", "LEATHER_HORSE_ARMOR")
    val customModelData = buttons?.getInt("customModelData", 0)
    val inputSlot: Int = buttons?.getInt("inputSlot", 10) ?: 10
    val outputSlot: Int = buttons?.getInt("outputSlot", 16) ?: 16
    val baseColorGrid: BaseColorGrid = buttons?.getConfigurationSection("baseColorGrid")?.getBaseColorGrid() ?: BaseColorGrid(12..14, 21..23, 30..32)
    val subColorRow: IntRange = buttons?.getString("subColorRow")?.toIntRange() ?: 46..52
    val effectButtonSlot: Int = buttons?.getInt("effectButton", 41) ?: 41
    private val blacklist = config.getConfigurationSection("blacklist")
    val blacklistedOraxen: List<String> = blacklist?.getStringList("oraxenItems") ?: emptyList()
    val blacklistedCrucible: List<String> = blacklist?.getStringList("crucibleItems") ?: emptyList()
    val blacklistedItemsAdder: List<String> = blacklist?.getStringList("itemsadderItems") ?: emptyList()
    val blacklistedLooty: List<String> = blacklist?.getStringList("lootyItems") ?: emptyList()
    val blacklistedTypes: List<String> = blacklist?.getStringList("types") ?: emptyList()
    val colors = config.getConfigurationSection("colors")?.getColors() ?: emptySet()
    val effectItem = config.getConfigurationSection("effectItem")?.getEffectItem()
    val effects = if (effectsEnabled) config.getConfigurationSection("effects")?.getEffectsColors() ?: emptySet() else emptySet()

    fun reload() {
        hmcColor.reloadConfig()
        colorConfig = HMCColorConfig()
    }

    private fun ConfigurationSection.getEffectItem(): ItemStack {
        this.getString("oraxenItem")?.let {
            if (isOraxenLoaded && it.isOraxenItem())
                return it.getOraxenItem()!!
        }
        this.getString("lootyItem")?.let {
            if (isLootyLoaded && it.isLootyItem())
                return it.getLootyItem()!!
        }
        this.getString("itemsadderItem")?.let {
            if (isIALoaded && it.isItemsAdderItem())
                return it.getItemsAdderStack()!!
        }
        this.getString("crucibleItem")?.let {
            if (isCrucibleLoaded && it.isCrucibleItem())
                return it.getCrucibleItem()!!
        }

        val itemStack = ItemStack(Material.AIR)
        val type = this.getString("type", "LEATHER_HORSE_ARMOR") ?: "LEATHER_HORSE_ARMOR"
        val material =
            if (type !in Material.values().map { it.name }) Material.LEATHER_HORSE_ARMOR
            else Material.valueOf(type)
        val name = this.getString("name", "")
        val color = this.getString("color", "#FFFFFF")?.toColor()
        val cmd = this.getInt("customModelData", customModelData ?: 0)
        val lore = this.getStringList("lore")

        itemStack.type = material
        itemStack.itemMeta = itemStack.itemMeta?.apply {
            name?.let { setDisplayName(name.toLegacy()) } //TODO Make subColor a map and add option for name?
            color?.let {
                (this as? LeatherArmorMeta)?.setColor(color)
                    ?: (this as? PotionMeta)?.setColor(color)
                    ?: (this as? FireworkMeta)?.setColor(color)
                    ?: (this as? FireworkEffectMeta)?.setColor(color)
                    ?: (this as? MapMeta)?.setColor(color)
            }
            this.lore = lore
            setCustomModelData(cmd)
        }

        return itemStack
    }

    private fun ConfigurationSection.getEffectsColors(): Set<Effects> {
        return this.getKeys(false).map {
            this.getConfigurationSection(it).let { c ->
                Effects(c?.getString("name", "") ?: "", c?.getString("color") ?: "#FFFFFF", c?.getString("permission"))
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

    private fun ConfigurationSection.getBaseColorGrid(): BaseColorGrid {
        return BaseColorGrid(this.getString("first", "")!!.toIntRange(), this.getString("second", "")!!.toIntRange(), this.getString("third", "")!!.toIntRange())
    }

    private fun String.toIntRange(): IntRange {
        this.split("..", limit = 2).let {
            return (it.firstOrNull()?.toInt() ?: 0)..(it.lastOrNull()?.toInt() ?: 0)
        }
    }
}


