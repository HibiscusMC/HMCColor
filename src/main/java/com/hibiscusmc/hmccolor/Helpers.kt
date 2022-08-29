package com.hibiscusmc.hmccolor

import dev.lone.itemsadder.api.CustomStack
import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import io.th0rgal.oraxen.items.OraxenItems
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import java.util.logging.Level

fun ItemStack.isOraxenItem() = OraxenItems.getIdByItem(this) != null
fun ItemStack.getOraxenID() = OraxenItems.getIdByItem(this)

fun ItemStack.isItemsAdderItem() = CustomStack.byItemStack(this) != null
fun ItemStack.getItemsAdderStack() = CustomStack.byItemStack(this)

fun String.miniMsg() = mm.deserialize(this)
fun Component.serialize() = mm.serialize(this)

fun <T> T.logVal(message: String = ""): T =
    hmcColor.logger.log(Level.INFO, "${if (message == "") "" else "$message: "}$this").let { this }

fun <T> T.broadcastVal(message: String = ""): T = broadcastMessage("$message$this").let { this }

fun ItemStack.setCustomModelData(int: Int): ItemStack {
    val meta = itemMeta
    meta?.setCustomModelData(int)
    this.itemMeta = meta
    return this
}

private fun ItemStack.isDyeable(): Boolean {
    if (itemMeta !is LeatherArmorMeta && itemMeta !is PotionMeta) return false
    return when {
        OraxenItems.exists(this) -> return OraxenItems.getIdByItem(this) !in colorConfig.blacklistedOraxen
        CustomStack.byItemStack(this) != null -> return CustomStack.byItemStack(this)?.id !in colorConfig.blacklistedItemsAdder
        else -> type.toString() !in colorConfig.blacklistedTypes
    }
}

fun createGui(): Gui {
    val rows = 6
    val gui = Gui.gui(GuiType.CHEST).rows(rows).title(colorConfig.title.miniMsg()).create()

    // baseColor square
    cachedDyeMap.map { it.key }.forEachIndexed { index, guiItem ->
        if (index < 3) gui.setItem(rows - 4, index + 4, guiItem)
        else if (index < 6) gui.setItem(rows - 3, index + 1, guiItem)
        else gui.setItem(rows - 2, index - 2, guiItem)
    }

    //TODO Add functionality for when you click the slots etc
    gui.guiItems.forEach { (_, clickedItem) ->
        clickedItem.setAction { click ->
            // Logic for clicking a baseColor to show all subColors
            cachedDyeMap[clickedItem]!!.map { it.itemStack.type }.logVal()
            when {
                clickedItem in cachedDyeMap.keys && click.isLeftClick ->
                    gui.filler.fillBetweenPoints(rows, 2, rows, 8, cachedDyeMap[clickedItem]!!)
                else -> return@setAction
            }
        }
    }

    //TODO Cancel topclick if item isnt dyeable
    gui.setDragAction { it.isCancelled = true }
    gui.setOutsideClickAction { it.isCancelled = true }
    gui.setDefaultTopClickAction {
        when {
            it.slot != 19 && it.slot != 25 -> it.isCancelled = true // Cancel any non input/output slot
            it.slot == 25 && it.currentItem == null -> it.isCancelled = true // Cancel adding items to empty output slot
            !it.isLeftClick || it.isShiftClick -> it.isCancelled = true // Cancel everything but leftClick action
            it.cursor != null && !it.cursor!!.isDyeable() -> it.isCancelled =
                true // Cancel adding non-dyeable or banned items
        }
    }

    gui.setCloseGuiAction {
        val inputItem = it.inventory.getItem(19) ?: return@setCloseGuiAction

        if (it.player.inventory.firstEmpty() != -1) {
            it.player.inventory.addItem(inputItem)
        } else it.player.world.dropItemNaturally(it.player.location, inputItem)
    }


    return gui
}

private fun String.toColor(): Color {
    return when {
        this.startsWith("#") -> return Color.fromRGB(this.substring(1).toInt(16))
        this.startsWith("0x") -> return Color.fromRGB(this.substring(2).toInt(16))
        "," in this -> {
            val colorString = this.replace(" ", "").split(",")
            if (colorString.any { it.toIntOrNull() == null }) return Color.WHITE
            Color.fromRGB(colorString[0].toInt(), colorString[1].toInt(), colorString[2].toInt())
        }
        //TODO Make this support text, probably through minimessage
        else -> return Color.WHITE
    }
}

fun getDyeColorList(): MutableMap<GuiItem, MutableList<GuiItem>> {
    val map = mutableMapOf<GuiItem, MutableList<GuiItem>>()

    colorConfig.colors.forEach baseColor@{ (key, colors) ->
        val list = mutableListOf<GuiItem>()
        val baseItem = ItemStack(Material.LEATHER_HORSE_ARMOR)

        baseItem.itemMeta = (baseItem.itemMeta as? LeatherArmorMeta ?: return@baseColor).apply {
            setColor(colors.baseColor.toColor())
            setDisplayName(Adventure.MINI_MESSAGE.deserialize(key.lowercase().replaceFirstChar{it.uppercase()}).serialize())
        }


        // Make the ItemStacks for all subColors
        colors.subColors.forEach subColor@{ color ->
            val subItem = ItemStack(Material.LEATHER_HORSE_ARMOR)
            subItem.itemMeta = (subItem.itemMeta as? LeatherArmorMeta ?: return@baseColor).apply {
                setDisplayName(color) //TODO Make subColor a map and add option for name?
                setColor(color.toColor())
            }

            if (list.size >= 7) return@subColor // Only allow for 7 subColor options
            val guiSub = GuiItem(subItem)

            guiSub.setAction {

            }

            list.add(guiSub)
        }
        if (map.size >= 9) return@baseColor // only show the first 9 baseColors

        val guiBase = GuiItem(baseItem)
        map[guiBase] = list
    }
    return map
}
