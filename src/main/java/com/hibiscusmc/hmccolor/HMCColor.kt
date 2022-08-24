package com.hibiscusmc.hmccolor

import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import me.mattstudios.mf.base.CommandManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.plugin.java.JavaPlugin

val hmcColor = Bukkit.getPluginManager().getPlugin("HMCColor") as JavaPlugin
val mm = MiniMessage.miniMessage()
var hmcColorGui: Gui? = null
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        hmcColorGui = createGui()

        CommandManager(hmcColor).register(HMCColorCommands())
    }

    override fun onDisable() {

    }

    private fun createGui(): Gui {
        val rows = 6
        val dyes = getDyeColorList()
        val gui = Gui.gui(GuiType.CHEST).rows(rows).title(colorConfig.title.miniMsg()).create()
        val fillerItem = ItemStack(Material.AIR)
        fillerItem.itemMeta = fillerItem.itemMeta?.apply { setCustomModelData(2) }

        gui.setItem(3, 2, GuiItem(fillerItem))
        gui.setItem(3, 8, GuiItem(fillerItem))
        //gui.setItem(2, 5, GuiItem(ItemStack(Material.PAPER).setCustomModelData(2)))

        // baseColor square
        dyes.map { it.key }.forEachIndexed { index, guiItem ->
            if (index < 3) gui.setItem(rows - 4, index + 4, guiItem)
            else if (index < 6) gui.setItem(rows - 3, index + 1, guiItem)
            else gui.setItem(rows - 2, index - 2, guiItem)
        }

        // Fill the subColor bar with the colors tied to the first baseColor
        gui.filler.fillBetweenPoints(rows, 2, rows, 8, dyes.values.firstOrNull() ?: listOf(GuiItem(fillerItem)))

        //TODO Add functionality for when you click the slots etc
        gui.guiItems.forEach { (_, clickedItem) ->
            clickedItem.setAction {
                when {
                    clickedItem == GuiItem(Material.STONE) -> it.isCancelled = true
                    clickedItem in dyes.keys && it.isLeftClick -> {
                        it.isCancelled = true
                        gui.filler.fillBetweenPoints(rows, 2, rows, 8, dyes[clickedItem] ?: return@setAction)
                    }

                    else -> {
                        val item = gui.getGuiItem(20)?.itemStack ?: return@setAction
                        val meta = item.itemMeta as? LeatherArmorMeta ?: return@setAction
                        meta.setColor((clickedItem.itemStack.itemMeta as? LeatherArmorMeta)?.color ?: return@setAction)
                        item.itemMeta = meta
                        gui.setItem(26, GuiItem(item))
                    }
                }
            }
        }
        return gui
    }

    private fun String.toColor(): Color {
        return when {
            this.startsWith("#") -> return Color.fromRGB(this.substring(1).toInt(16))
            this.startsWith("0x") -> return Color.fromRGB(this.substring(2).toInt(16))
            "," in this -> {
                val colorString = this.replace(" ", "").split(",")
                Color.fromRGB(colorString[0].toInt(), colorString[1].toInt(), colorString[2].toInt())
            }

            else -> return Color.fromRGB(this.toInt(16))
        }
    }

    private fun getDyeColorList(): MutableMap<GuiItem, MutableList<GuiItem>> {
        val map = mutableMapOf<GuiItem, MutableList<GuiItem>>()
        val list = mutableListOf<GuiItem>()

        colorConfig.colors.forEach baseColor@{ (key, colors) ->
            val dyeItem = ItemStack(Material.LEATHER_HORSE_ARMOR)
            val baseMeta = dyeItem.clone().itemMeta as? LeatherArmorMeta ?: return@baseColor

            // Make the baseColor ItemStack
            baseMeta.setDisplayName(key.lowercase().replaceFirstChar { "<${key.lowercase()}>" + it.uppercase() })
            baseMeta.setColor(colors.baseColor.toColor())
            dyeItem.itemMeta = baseMeta
            list.clear()

            // Make the ItemStacks for all subColors
            colors.subColors.forEach subColor@{ color ->
                val item = dyeItem.clone()
                val meta = item.itemMeta as? LeatherArmorMeta ?: return@subColor

                meta.setColor(color.toColor())
                item.itemMeta = meta

                if (list.size >= 7) return@subColor // Only allow for 7 subColor options
                list.add(GuiItem(item))
            }

            if (map.size >= 9) return@baseColor // only show the first 9 baseColors
            if (list.isEmpty()) repeat(9) { list.add(GuiItem(Material.AIR)) }
            map[GuiItem(dyeItem)] = list
        }
        return map
    }
}
