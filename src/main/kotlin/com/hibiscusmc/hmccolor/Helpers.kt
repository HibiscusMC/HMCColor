package com.hibiscusmc.hmccolor

import com.mineinabyss.geary.papermc.GearyPlugin
import com.mineinabyss.geary.papermc.datastore.decodePrefabs
import com.mineinabyss.geary.papermc.tracking.items.itemTracking
import com.mineinabyss.geary.prefabs.PrefabKey
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.plugin.Plugins
import com.mineinabyss.idofront.textcomponents.miniMsg
import dev.lone.itemsadder.api.CustomStack
import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import io.lumine.mythiccrucible.MythicCrucible
import io.th0rgal.oraxen.OraxenPlugin
import io.th0rgal.oraxen.api.OraxenItems
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.inventory.meta.PotionMeta
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun ItemStack.isOraxenItem() = OraxenItems.exists(this)
fun ItemStack.getOraxenID(): String? = OraxenItems.getIdByItem(this)
fun String.isOraxenItem() = OraxenItems.exists(this)
fun String.getOraxenItem(): ItemStack? = OraxenItems.getItemById(this).build()

fun ItemStack.isCrucibleItem() = crucible.itemManager.getItem(this).isPresent
fun ItemStack.getCrucibleId(): String? = crucible.itemManager.getItem(this).get().internalName
fun String.isCrucibleItem() = crucible.itemManager.getItem(this).isPresent
fun String.getCrucibleItem(): ItemStack? = MythicCrucible.core().itemManager.getItemStack(this)

fun ItemStack.isItemsAdderItem() = CustomStack.byItemStack(this) != null
fun ItemStack.getItemsAdderID() = CustomStack.byItemStack(this)?.namespacedID
fun String.isItemsAdderItem() = CustomStack.isInRegistry(this)
fun String.getItemsAdderStack() = CustomStack.getInstance(this)?.itemStack

fun ItemStack.isGearyItem() = this.itemMeta?.persistentDataContainer?.decodePrefabs()?.first()?.let { itemTracking.createItem(it) != null } ?: false
fun ItemStack.getGearyID() = this.itemMeta?.persistentDataContainer?.decodePrefabs()?.first()?.full
fun String.isGearyItem() = PrefabKey.ofOrNull(this)?.let { itemTracking.createItem(it) != null } ?: false
fun String.getGearyItem() = PrefabKey.ofOrNull(this)?.let { itemTracking.createItem(it) }

val isIALoaded = Plugins.isEnabled("ItemsAdder")
val isOraxenLoaded = Plugins.isEnabled<OraxenPlugin>()
val isCrucibleLoaded = Plugins.isEnabled<MythicCrucible>()
val isGearyLoaded = Plugins.isEnabled<GearyPlugin>()

private fun ItemStack.isDyeable(): Boolean {
    if (itemMeta !is LeatherArmorMeta && itemMeta !is PotionMeta && itemMeta !is MapMeta) return false
    hmcColor.config.blacklistedItems.let { blacklist ->
        return when {
            isOraxenLoaded && this.isOraxenItem() -> this.getOraxenID() !in blacklist.oraxenItems
            isCrucibleLoaded && this.isCrucibleItem() -> this.getCrucibleId() !in blacklist.crucibleItems
            isIALoaded && this.isItemsAdderItem() -> this.getItemsAdderID() !in blacklist.itemsadderItems
            isGearyLoaded && this.isGearyItem() -> this.getGearyID() !in blacklist.gearyItems
            else -> type !in blacklist.types
        }
    }
}

// Confusing but slot is sometimes 19 sometimes 20 due to inventory starting at index 0 whilst gui at 1
fun createGui(): Gui {
    var effectToggleState = false
    val rows = 6
    val gui = Gui.gui(GuiType.CHEST).rows(rows).title(hmcColor.config.title.miniMsg()).create()

    // baseColor square
    hmcColor.config.buttons.baseColorGrid.let {
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
    val effectItem = if (cachedEffectSet.isNotEmpty()) GuiItem(hmcColor.config.effectItem.toItemStackOrNull() ?: defaultItem) else null
    effectItem?.let { gui.setItem(hmcColor.config.buttons.effectButton, it) }

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
                    hmcColor.config.buttons.subColorRow.forEach { gui.updateItem(it, GuiItem(Material.AIR)) }
                    // Find the middle of given IntRange
                    val middleSubColor = hmcColor.config.buttons.subColorRow.first + hmcColor.config.buttons.subColorRow.count() / 2
                    // Subtract 0.1 because we want to round down on .5
                    val offset = (dyeMap.size / 2.0 - 0.1).roundToInt()
                    val range = max(middleSubColor - offset, hmcColor.config.buttons.subColorRow.first)..min(middleSubColor + offset, hmcColor.config.buttons.subColorRow.last)
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
                                    val guiInput = click.inventory.getItem(hmcColor.config.buttons.inputSlot)?.let { i -> GuiItem(i) } ?: return@subAction
                                    val guiOutput = GuiItem(guiInput.itemStack.clone())

                                    guiOutput.itemStack.editItemMeta {
                                        val appliedColor = subColor.itemStack.itemMeta?.let { meta ->
                                            when (meta) {
                                                is LeatherArmorMeta -> meta.color
                                                is PotionMeta -> meta.color
                                                is MapMeta -> meta.color
                                                else -> null
                                            }
                                        } ?: return@subAction

                                        hmcColor.config.effects.values.find { e -> e.color.toColor() == appliedColor }?.let { effect ->
                                            effect.permission?.let { perm ->
                                                if (!click.whoClicked.hasPermission(perm)) return@subAction
                                            }
                                        }

                                        when (this) {
                                            is LeatherArmorMeta -> setColor(appliedColor)
                                            is PotionMeta -> this.color = appliedColor
                                            is MapMeta -> this.color = appliedColor
                                            else -> return@subAction
                                        }
                                    }

                                    gui.setItem(hmcColor.config.buttons.outputSlot, guiOutput)
                                    gui.updateItem(hmcColor.config.buttons.outputSlot, guiOutput)
                                    guiOutput.setAction output@{ click ->
                                        when {
                                            click.isCancelled -> return@output
                                            click.cursor?.type == Material.AIR && click.currentItem != null -> {
                                                click.isCancelled = true
                                                if (!click.isShiftClick) click.whoClicked.setItemOnCursor(click.currentItem)
                                                else click.currentItem?.let { current -> click.whoClicked.inventory.addItem(current) }
                                                gui.updateItem(hmcColor.config.buttons.inputSlot, ItemStack(Material.AIR))
                                                gui.updateItem(hmcColor.config.buttons.outputSlot, ItemStack(Material.AIR))
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
            val inputStack = gui.getGuiItem(hmcColor.config.buttons.inputSlot)?.itemStack
            if (inputStack == null || inputStack.type.isAir && click.currentItem?.isDyeable() == true) {
                click.isCancelled = true
                gui.updateItem(hmcColor.config.buttons.inputSlot, GuiItem(click.currentItem!!))
                gui.update()
                click.whoClicked.inventory.setItem(click.slot, ItemStack(Material.AIR))
            } else click.isCancelled = true
        }
    }
    gui.setDefaultTopClickAction { click ->
        when {
            click.slot == hmcColor.config.buttons.inputSlot && click.whoClicked.itemOnCursor.type == Material.AIR && click.currentItem != null -> {
                click.isCancelled = true
                click.whoClicked.setItemOnCursor(click.inventory.getItem(hmcColor.config.buttons.inputSlot))
                gui.updateItem(hmcColor.config.buttons.inputSlot, ItemStack(Material.AIR))
                gui.updateItem(hmcColor.config.buttons.outputSlot, ItemStack(Material.AIR))
                gui.update()
            }

            click.slot !in hmcColor.config.buttons.let { c -> setOf(c.inputSlot, c.outputSlot, c.effectButton) } -> click.isCancelled = true // Cancel any non input/output/effectToggle slot
            click.slot == hmcColor.config.buttons.outputSlot && click.currentItem == null -> click.isCancelled = true // Cancel adding items to empty output slot
            click.slot != hmcColor.config.buttons.outputSlot && click.isShiftClick -> click.isCancelled = true // Cancel everything but leftClick action
            click.cursor?.type?.isAir == false && click.cursor?.isDyeable() == false -> click.isCancelled = true // Cancel adding non-dyeable or banned items
        }
    }

    gui.setCloseGuiAction { click ->
        val inputItem = click.inventory.getItem(hmcColor.config.buttons.inputSlot) ?: return@setCloseGuiAction

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
    return hmcColor.config.effects.values.map effectColor@{ effect ->
        val color = effect.color.toColor()
        GuiItem(defaultItem.editItemMeta {
            displayName(effect.name.miniMsg())
            when (this) {
                is LeatherArmorMeta -> this.setColor(color)
                is PotionMeta -> this.color = color
                is MapMeta -> this.color = color
            }
        })
    }.toMutableSet()
}

fun getDyeColorList(): MutableMap<GuiItem, MutableList<GuiItem>> {
    val map = mutableMapOf<GuiItem, MutableList<GuiItem>>()

    hmcColor.config.colors.values.forEach baseColor@{ (baseColor, subColors) ->
        val list = mutableListOf<GuiItem>()
        val baseItem = defaultItem

        baseItem.editItemMeta {
            displayName(baseColor.name.miniMsg())
            val color = baseColor.color.toColor()
            when (this) {
                is LeatherArmorMeta -> this.setColor(color)
                is PotionMeta -> this.color = color
                is MapMeta -> this.color = color
            }
        }


        // Make the ItemStacks for all subColors
        subColors.forEach subColor@{ subColor ->
            val subItem = hmcColor.config.buttons.item.toItemStackOrNull() ?: defaultItem

            subItem.editItemMeta {
                displayName(subColor.name.miniMsg())
                val color = subColor.color.toColor()
                when (this) {
                    is LeatherArmorMeta -> this.setColor(color)
                    is PotionMeta -> this.color = color
                    is MapMeta -> this.color = color
                }
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

private val defaultItem get() = hmcColor.config.buttons.item.toItemStackOrNull() ?: ItemStack(Material.LEATHER_HORSE_ARMOR)
