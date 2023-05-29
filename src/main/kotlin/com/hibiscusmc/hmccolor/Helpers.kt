package com.hibiscusmc.hmccolor

import com.hibiscusmc.hmccolor.Adventure.toLegacy
import com.mineinabyss.geary.prefabs.PrefabKey
import com.mineinabyss.idofront.serialization.toSerializable
import com.mineinabyss.looty.LootyFactory
import com.mineinabyss.looty.tracking.toGearyFromUUIDOrNull
import dev.lone.itemsadder.api.CustomStack
import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import io.lumine.mythiccrucible.MythicCrucible
import io.th0rgal.oraxen.api.OraxenItems
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.inventory.meta.PotionMeta
import java.util.logging.Level
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun ItemStack.isOraxenItem() = OraxenItems.exists(this)
fun ItemStack.getOraxenID() = OraxenItems.getIdByItem(this)
fun String.isOraxenItem() = OraxenItems.exists(this)
fun String.getOraxenItem() = OraxenItems.getItemById(this).build()

fun ItemStack.isCrucibleItem() = crucible.itemManager.getItem(this).isPresent
fun ItemStack.getCrucibleId() = crucible.itemManager.getItem(this).get().internalName
fun String.isCrucibleItem() = crucible.itemManager.getItem(this).isPresent
fun String.getCrucibleItem() = MythicCrucible.core().itemManager.getItemStack(this)

fun ItemStack.isItemsAdderItem() = CustomStack.byItemStack(this) != null
fun ItemStack.getItemsAdderID() = CustomStack.byItemStack(this)?.namespacedID
fun String.isItemsAdderItem() = CustomStack.isInRegistry(this)
fun String.getItemsAdderStack() = CustomStack.getInstance(this)?.itemStack

fun ItemStack.isLootyItem() = this.toGearyFromUUIDOrNull() != null
fun ItemStack.getLootyID() = this.toSerializable().prefab
fun String.isLootyItem() = PrefabKey.ofOrNull(this)?.let { LootyFactory.createFromPrefab(it) } != null
fun String.getLootyItem() = PrefabKey.ofOrNull(this)?.let { LootyFactory.createFromPrefab(it) }

fun String.miniMsg() = Adventure.MINI_MESSAGE.deserialize(this)
fun Component.serialize() = Adventure.MINI_MESSAGE.serialize(this)

val isIALoaded = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")
val isOraxenLoaded = Bukkit.getPluginManager().isPluginEnabled("Oraxen")
val isCrucibleLoaded = Bukkit.getPluginManager().isPluginEnabled("MythicCrucible")
val isLootyLoaded = Bukkit.getPluginManager().isPluginEnabled("Looty")

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
    if (itemMeta !is LeatherArmorMeta && itemMeta !is PotionMeta && itemMeta !is MapMeta && itemMeta !is FireworkMeta) return false
    return when {
        isOraxenLoaded && this.isOraxenItem() ->
            this.getOraxenID() !in colorConfig.blacklistedOraxen

        isCrucibleLoaded && this.isCrucibleItem() ->
            this.getCrucibleId() !in colorConfig.blacklistedCrucible

        isIALoaded && this.isItemsAdderItem() ->
            this.getItemsAdderID() !in colorConfig.blacklistedItemsAdder

        isLootyLoaded && this.isLootyItem() ->
            this.getLootyID() !in colorConfig.blacklistedLooty

        else -> type.toString() !in colorConfig.blacklistedTypes
    }
}

// Confusing but slot is sometimes 19 sometimes 20 due to inventory starting at index 0 whilst gui at 1
fun createGui(): Gui {
    var effectToggleState = false
    val rows = 6
    val gui = Gui.gui(GuiType.CHEST).rows(rows).title(colorConfig.title.miniMsg()).create()

    // baseColor square
    //TODO base of of config values

    colorConfig.baseColorGrid.let {
        it.first.forEachIndexed { index, int ->
            gui.setItem(int, cachedDyeMap.keys.elementAt(index))
        }
        it.second.forEachIndexed { index, int ->
            gui.setItem(int, cachedDyeMap.keys.elementAt(index + 3))
        }
        it.third.forEachIndexed { index, int ->
            gui.setItem(int, cachedDyeMap.keys.elementAt(index + 6))
        }
    }

    // Effects toggle
    val effectItem = if (cachedEffectSet.isNotEmpty()) GuiItem(colorConfig.effectItem ?: getDefaultItem()) else null
    effectItem?.let { gui.setItem(colorConfig.effectButtonSlot, it) }

    gui.guiItems.forEach { (_, clickedItem) ->
        clickedItem.setAction { click ->
            // Logic for clicking a baseColor to show all subColors
            when {
                click.isShiftClick -> return@setAction
                click.isLeftClick && (clickedItem in cachedDyeMap.keys || effectItem?.let { it == clickedItem } ?: return@setAction) -> {
                    val dyeMap: List<GuiItem> = when (clickedItem) {
                        effectItem -> {
                            click.isCancelled = true
                            effectToggleState = !effectToggleState
                            if (effectToggleState) cachedEffectSet.toList()
                            else cachedDyeMap.values.firstOrNull() ?: return@setAction
                        }

                        else -> {
                            effectToggleState = false
                            cachedDyeMap[clickedItem] ?: return@setAction
                        }
                    }


                    //Reset bottom
                    colorConfig.subColorRow.forEach { gui.updateItem(it, GuiItem(Material.AIR)) }
                    // Find the middle of given IntRange
                    val middleSubColor = colorConfig.subColorRow.first + colorConfig.subColorRow.count() / 2
                    // Subtract 0.1 because we want to round down on .5
                    val offset = (dyeMap.size / 2.0 - 0.1).roundToInt()
                    val range = max(middleSubColor - offset, colorConfig.subColorRow.first)..min(middleSubColor + offset, colorConfig.subColorRow.last)
                    range.forEachIndexed { index, i ->
                        gui.updateItem(
                            i, try {
                                // if effect is toggled, we fill based on effect list, otherwise its a dye color
                                dyeMap[index]
                            } catch (_: IndexOutOfBoundsException) {
                                GuiItem(Material.AIR)
                            }
                        )
                        val subColor = gui.getGuiItem(i) ?: return@forEachIndexed
                        subColor.setAction subAction@{
                            when {
                                it.isShiftClick -> return@subAction
                                (click.isLeftClick && (subColor in cachedDyeMap.values.flatten() || subColor in cachedEffectSet)) -> {
                                    val guiInput = click.inventory.getItem(colorConfig.inputSlot)?.let { i -> GuiItem(i) } ?: return@subAction
                                    val guiOutput = GuiItem(guiInput.itemStack.clone())

                                    guiOutput.itemStack.itemMeta = guiOutput.itemStack.itemMeta?.apply {
                                        val appliedColor = subColor.itemStack.itemMeta?.let { meta ->
                                            when (meta) {
                                                is LeatherArmorMeta -> meta.color
                                                is PotionMeta -> meta.color
                                                is MapMeta -> meta.color
                                                is FireworkMeta -> meta.color
                                                else -> null
                                            }
                                        } ?: return@subAction

                                        colorConfig.effects.find { e -> e.color.toColor() == appliedColor }?.let { effect ->
                                            effect.permission?.let { perm ->
                                                if (!click.whoClicked.hasPermission(perm)) return@subAction
                                            }
                                        }

                                        (this as? LeatherArmorMeta)?.setColor(appliedColor)
                                            ?: (this as? PotionMeta)?.setColor(appliedColor)
                                            ?: (this as? MapMeta)?.setColor(appliedColor) 
                                            ?: (this as? FireworkMeta)?.setColor(appliedColor) ?: return@apply
                                    }

                                    gui.setItem(colorConfig.outputSlot, guiOutput)
                                    gui.updateItem(colorConfig.outputSlot, guiOutput)
                                    guiOutput.setAction output@{ click ->
                                        when {
                                            click.isCancelled -> return@output
                                            click.cursor?.type == Material.AIR && click.currentItem != null -> {
                                                click.isCancelled = true
                                                if (!click.isShiftClick) click.whoClicked.setItemOnCursor(click.currentItem)
                                                else click.whoClicked.inventory.addItem(click.currentItem)
                                                gui.updateItem(colorConfig.inputSlot, ItemStack(Material.AIR))
                                                gui.updateItem(colorConfig.outputSlot, ItemStack(Material.AIR))
                                                gui.update()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> return@setAction
            }
        }
    }

    gui.setDragAction { it.isCancelled = true }
    gui.setOutsideClickAction { it.isCancelled = true }
    gui.setPlayerInventoryAction { click ->
        if (click.isShiftClick) {
            val inputStack = gui.getGuiItem(colorConfig.inputSlot)?.itemStack
            if (inputStack == null || inputStack.type.isAir && click.currentItem?.isDyeable() == true) {
                click.isCancelled = true
                gui.updateItem(colorConfig.inputSlot, GuiItem(click.currentItem!!))
                gui.update()
                click.whoClicked.inventory.setItem(click.slot, ItemStack(Material.AIR))
            } else click.isCancelled = true
        }
    }
    gui.setDefaultTopClickAction { click ->
        when {
            click.slot == colorConfig.inputSlot && click.whoClicked.itemOnCursor.type == Material.AIR && click.currentItem != null -> {
                click.isCancelled = true
                click.whoClicked.setItemOnCursor(click.inventory.getItem(colorConfig.inputSlot))
                gui.updateItem(colorConfig.inputSlot, ItemStack(Material.AIR))
                gui.updateItem(colorConfig.outputSlot, ItemStack(Material.AIR))
                gui.update()
            }

            click.slot !in colorConfig.let { c -> setOf(c.inputSlot, c.outputSlot, c.effectButtonSlot) } -> click.isCancelled = true // Cancel any non input/output/effectToggle slot
            click.slot == colorConfig.outputSlot && click.currentItem == null -> click.isCancelled = true // Cancel adding items to empty output slot
            click.slot != colorConfig.outputSlot && click.isShiftClick -> click.isCancelled = true // Cancel everything but leftClick action
            click.cursor?.type?.isAir == false && click.cursor?.isDyeable() == false -> click.isCancelled = true // Cancel adding non-dyeable or banned items
        }
    }

    gui.setCloseGuiAction { click ->
        val inputItem = click.inventory.getItem(colorConfig.inputSlot) ?: return@setCloseGuiAction

        if (click.player.inventory.firstEmpty() != -1) {
            click.player.inventory.addItem(inputItem)
        } else click.player.world.dropItemNaturally(click.player.location, inputItem)
    }


    return gui
}

internal fun String.toColor(): Color {
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

fun getEffectList(): MutableSet<GuiItem> {
    return colorConfig.effects.map effectColor@{ effect ->
        val effectItem = getDefaultItem()
        val color = effect.color.toColor()
        effectItem.itemMeta = effectItem.itemMeta?.apply {
            (this as? LeatherArmorMeta)?.setColor(color)
                ?: (this as? PotionMeta)?.setColor(color)
                ?: (this as? MapMeta)?.setColor(color)
                ?: (this as? FireworkMeta)?.setColor(color)
            setDisplayName(effect.name.toLegacy())
        }

        GuiItem(effectItem)
    }.toMutableSet()
}

fun getDyeColorList(): MutableMap<GuiItem, MutableList<GuiItem>> {
    val map = mutableMapOf<GuiItem, MutableList<GuiItem>>()

    colorConfig.colors.forEach baseColor@{ (baseColor, subColors) ->
        val list = mutableListOf<GuiItem>()
        val baseItem = getDefaultItem()

        baseItem.itemMeta = baseItem.itemMeta?.apply {
            val color = baseColor.color.toColor()
            (this as? LeatherArmorMeta)?.setColor(color)
                ?: (this as? PotionMeta)?.setColor(color)
                ?: (this as? MapMeta)?.setColor(color)
                ?: (this as? FireworkMeta)?.setColor(color) ?: return@baseColor
            setDisplayName(baseColor.name.toLegacy())
        } ?: return@baseColor


        // Make the ItemStacks for all subColors
        subColors.forEach subColor@{ subColor ->
            val subItem = when {
                isOraxenLoaded && colorConfig.oraxenItem?.isOraxenItem() == true ->
                    colorConfig.oraxenItem?.getOraxenItem() ?: getDefaultItem()

                isCrucibleLoaded && colorConfig.crucibleItem?.isCrucibleItem() == true ->
                    colorConfig.crucibleItem?.getCrucibleItem() ?: getDefaultItem()

                isIALoaded && colorConfig.itemsAdderItem?.isItemsAdderItem() == true ->
                    colorConfig.itemsAdderItem?.getItemsAdderStack() ?: getDefaultItem()

                isLootyLoaded && colorConfig.lootyItem?.isLootyItem() == true ->
                    colorConfig.lootyItem?.getLootyItem() ?: getDefaultItem()

                else -> getDefaultItem()
            }

            subItem.itemMeta = subItem.itemMeta?.apply {
                setDisplayName(subColor.name.toLegacy())
                val color = subColor.color.toColor()
                (this as? LeatherArmorMeta)?.setColor(color)
                    ?: (this as? PotionMeta)?.setColor(color)
                    ?: (this as? MapMeta)?.setColor(color)
                    ?: (this as? FireworkMeta)?.setColor(color) ?: return@baseColor
            }

            if (list.size >= 7) return@subColor // Only allow for 7 subColor options
            val guiSub = GuiItem(subItem)
            list.add(guiSub)
        }
        if (map.size >= 9) return@baseColor // only show the first 9 baseColors

        val guiBase = GuiItem(baseItem)
        map[guiBase] = list
    }
    return map
}

private fun getDefaultItem(): ItemStack {
    val material =
        Material.getMaterial(colorConfig.defaultItem ?: "LEATHER_HORSE_ARMOR") ?: Material.LEATHER_HORSE_ARMOR
    val item = ItemStack(material)
    item.itemMeta = item.itemMeta?.apply { setCustomModelData(colorConfig.customModelData) }
    return item
}
