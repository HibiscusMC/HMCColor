package com.hibiscusmc.hmccolor

import dev.lone.itemsadder.api.CustomStack
import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import io.th0rgal.oraxen.items.OraxenItems
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta

fun ItemStack.isOraxenItem() = OraxenItems.getIdByItem(this) != null
fun ItemStack.getOraxenID() = OraxenItems.getIdByItem(this)

fun ItemStack.isItemsAdderItem() = CustomStack.byItemStack(this) != null
fun ItemStack.getItemsAdderStack() = CustomStack.byItemStack(this)

fun String.miniMsg() = mm.deserialize(this)
fun Component.serialize() = mm.serialize(this)

fun Any.broadcastVal(prefix: String = "") = Bukkit.broadcastMessage(prefix + this.toString())

fun ItemStack.setCustomModelData(int: Int) : ItemStack {
    val meta = itemMeta
    meta?.setCustomModelData(int)
    this.itemMeta = meta
    return this
}

fun ItemStack.isDyeable() : Boolean {
    if (itemMeta !is LeatherArmorMeta && itemMeta !is PotionMeta) return false
    return when {
        OraxenItems.exists(this) -> return OraxenItems.getIdByItem(this) !in colorConfig.blacklistedOraxen
        CustomStack.byItemStack(this) != null -> return CustomStack.byItemStack(this)?.id !in colorConfig.blacklistedItemsAdder
        else -> type.toString() !in colorConfig.blacklistedTypes
    }
}

fun createGui(): Gui {
    val rows = 6
    val dyes = getDyeColorList()
    val gui = Gui.gui(GuiType.CHEST).rows(rows).title(colorConfig.title.miniMsg()).create()

    // baseColor square
    dyes.map { it.key }.forEachIndexed { index, guiItem ->
        if (index < 3) gui.setItem(rows - 4, index + 4, guiItem)
        else if (index < 6) gui.setItem(rows - 3, index + 1, guiItem)
        else gui.setItem(rows - 2, index - 2, guiItem)
    }

    //TODO Cancel topclick if item isnt dyeable
    gui.setDragAction { it.isCancelled = true }
    gui.setOutsideClickAction { it.isCancelled = true }
    gui.setDefaultTopClickAction {
        if (it.slot != 19 && it.slot != 25) it.isCancelled = true
        else if (!it.isLeftClick || it.isShiftClick) it.isCancelled = true
        else if (it.cursor != null && !it.cursor!!.isDyeable()) it.isCancelled = true
    }

    gui.setCloseGuiAction {
        val inputItem = it.inventory.getItem(19) ?: return@setCloseGuiAction

        if (it.player.inventory.firstEmpty() != -1) {
            it.player.inventory.addItem(inputItem)
        } else it.player.world.dropItemNaturally(it.player.location, inputItem)
    }

    //TODO Add functionality for when you click the slots etc
    gui.guiItems.forEach { (_, clickedItem) ->
        clickedItem.setAction { click ->
            when {
                clickedItem in dyes.keys && click.isLeftClick -> {
                    gui.filler.fillBetweenPoints(rows, 2, rows, 8, dyes[clickedItem] ?: return@setAction)
                    click.isCancelled = true
                }

                dyes.values.any { clickedItem in it } -> {
                    val item = gui.getGuiItem(19)?.itemStack ?: return@setAction
                    (item.itemMeta as? LeatherArmorMeta ?: return@setAction).apply {
                        this.setColor((clickedItem.itemStack.itemMeta as? LeatherArmorMeta)?.color ?: return@setAction)
                    }

                    gui.setItem(25, GuiItem(item))
                    click.isCancelled = true
                }
                else -> return@setAction
            }
        }
    }
    return gui
}

fun String.toColor(): Color {
    return when {
        this.startsWith("#") -> return Color.fromRGB(this.substring(1).toInt(16))
        this.startsWith("0x") -> return Color.fromRGB(this.substring(2).toInt(16))
        "," in this -> {
            val colorString = this.replace(" ", "").split(",")
            if (colorString.any { it.toIntOrNull() == null }) return Color.WHITE
            Color.fromRGB(colorString[0].toInt(), colorString[1].toInt(), colorString[2].toInt())
        }

        else -> return Color.fromRGB(this.toInt(16))
    }
}

fun getDyeColorList(): MutableMap<GuiItem, MutableList<GuiItem>> {
    val map = mutableMapOf<GuiItem, MutableList<GuiItem>>()
    val list = mutableListOf<GuiItem>()

    colorConfig.colors.forEach baseColor@{ (key, colors) ->
        // Make the baseColor ItemStack
        val baseItem = ItemStack(Material.LEATHER_HORSE_ARMOR)
        baseItem.itemMeta = (baseItem.itemMeta as? LeatherArmorMeta ?: return@baseColor).apply {
            setDisplayName("" + key.lowercase().replaceFirstChar { it.uppercase() })
            setColor(colors.baseColor.toColor())
        }

        // Make the ItemStacks for all subColors
        colors.subColors.forEach subColor@{ color ->
            val subItem = ItemStack(Material.LEATHER_HORSE_ARMOR)
            subItem.itemMeta = (subItem.itemMeta as? LeatherArmorMeta ?: return@baseColor).apply {
                setDisplayName(" ")
                setColor(color.toColor())
            }

            if (list.size >= 7) return@subColor // Only allow for 7 subColor options
            list.add(GuiItem(subItem))
        }

        if (map.size >= 9) return@baseColor // only show the first 9 baseColors
        map[GuiItem(baseItem)] = list
        list.clear()
    }
    return map
}
