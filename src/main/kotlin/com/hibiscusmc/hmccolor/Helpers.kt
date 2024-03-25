@file:OptIn(ExperimentalStdlibApi::class)

package com.hibiscusmc.hmccolor

import com.mineinabyss.geary.papermc.datastore.decodePrefabs
import com.mineinabyss.geary.papermc.tracking.items.gearyItems
import com.mineinabyss.geary.prefabs.PrefabKey
import com.mineinabyss.idofront.items.Colorable
import com.mineinabyss.idofront.items.asColorable
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.plugin.Plugins
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.util.ColorHelpers
import dev.lone.itemsadder.api.CustomStack
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import io.lumine.mythiccrucible.MythicCrucible
import io.th0rgal.oraxen.api.OraxenItems
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun ItemStack.isOraxenItem() = Plugins.isEnabled("Oraxen") && OraxenItems.exists(this)
fun ItemStack.oraxenID(): String? = if (Plugins.isEnabled("Oraxen")) OraxenItems.getIdByItem(this) else null
fun String.isOraxenItem() = Plugins.isEnabled("Oraxen") && OraxenItems.exists(this)
fun String.oraxenItem(): ItemStack? = if (Plugins.isEnabled("Oraxen")) OraxenItems.getItemById(this).build() else null

fun ItemStack.isCrucibleItem() = Plugins.isEnabled("MythicCrucible") && crucible.itemManager.getItem(this).isPresent
fun ItemStack.crucibleID(): String? = if (Plugins.isEnabled("MythicCrucible")) crucible.itemManager.getItem(this).get().internalName else null
fun String.isCrucibleItem() = Plugins.isEnabled("MythicCrucible") && crucible.itemManager.getItem(this).isPresent
fun String.crucibleItem(): ItemStack? = if (Plugins.isEnabled("MythicCrucible")) MythicCrucible.core().itemManager.getItemStack(this) else null

fun ItemStack.isItemsAdderItem() = Plugins.isEnabled("ItemsAdder") && CustomStack.byItemStack(this) != null
fun ItemStack.itemsAdderID() = if (Plugins.isEnabled("ItemsAdder")) CustomStack.byItemStack(this)?.namespacedID else null
fun String.isItemsAdderItem() = Plugins.isEnabled("ItemsAdder") && CustomStack.isInRegistry(this)
fun String.itemsAdderItem() = if (Plugins.isEnabled("ItemsAdder")) CustomStack.getInstance(this)?.itemStack else null

fun ItemStack.isGearyItem() = Plugins.isEnabled("Geary") &&
    this.itemMeta?.persistentDataContainer?.decodePrefabs()?.firstOrNull()?.let { gearyItems.createItem(it) != null } ?: false

fun ItemStack.gearyID() = this.itemMeta?.persistentDataContainer?.decodePrefabs()?.first()?.full
fun String.isGearyItem() = Plugins.isEnabled("Geary") && PrefabKey.ofOrNull(this)?.let { gearyItems.createItem(it) != null } ?: false
fun String.gearyItem() = if (Plugins.isEnabled("Geary")) PrefabKey.ofOrNull(this)?.let { gearyItems.createItem(it) } else null

private fun ItemStack.isDyeable(): Boolean {
    val blacklist = hmcColor.config.blacklistedItems
    return when {
        (itemMeta as? Colorable) != null -> false
        this.isOraxenItem() -> this.oraxenID() !in blacklist.oraxenItems
        this.isCrucibleItem() -> this.crucibleID() !in blacklist.crucibleItems
        this.isItemsAdderItem() -> this.itemsAdderID() !in blacklist.itemsadderItems
        this.isGearyItem() -> this.gearyID() !in blacklist.gearyItems
        else -> type !in blacklist.types
    }
}

val baseColorScrollingIndex: MutableMap<UUID, Int> = mutableMapOf()
val subColorScrollingIndex: MutableMap<UUID, Int> = mutableMapOf()

fun Player.createColorMenu(): Gui {
    val gui = Gui.gui(GuiType.CHEST).rows(hmcColor.config.rows).title(hmcColor.config.title.miniMsg()).create()
    val cachedDyeMap = dyeColorItemMap(this)
    val cachedEffectSet = effectItemList(this)
    var effectToggleState = false

    val buttons = hmcColor.config.buttons
    val baseColorGrid = buttons.baseColorGrid

    baseColorScrollingIndex[uniqueId] = 0 // Reset if player was in before
    subColorScrollingIndex[uniqueId] = 0 // Reset if player was in before

    when (baseColorGrid.type) {
        HMCColorConfig.BaseColorGrid.Type.NORMAL -> baseColorGrid.normalGrid.rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { index, int ->
                gui.setItem(int, cachedDyeMap.keys.elementAt(index + 3 * rowIndex).also { item ->
                    item.setAction {
                        effectToggleState = false
                        val dyeMap = cachedDyeMap[item] ?: return@setAction
                        fillSubColorRow(gui, this, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
                        gui.clearOutputItem()
                    }
                })
            }
        }
        HMCColorConfig.BaseColorGrid.Type.SCROLLING -> {
            val baseRow = baseColorGrid.scrollingGrid.row
            cachedDyeMap.keys.toList().zip(baseRow).forEach { (item, slot) ->
                item.setAction {
                    effectToggleState = false
                    val dyeMap = cachedDyeMap[item] ?: return@setAction
                    fillSubColorRow(gui, this, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
                    gui.clearOutputItem()
                }
                gui.updateItem(slot, item)
                gui.clearOutputItem()
            }

            val (backwardSlot, scrollBackward) = baseColorGrid.scrollingGrid.let { it.backwardSlot to (it.backwardItem.toItemStackOrNull() ?: defaultItem) }
            val (forwardSlot, scrollForward) = baseColorGrid.scrollingGrid.let { it.forwardSlot to (it.forwardItem.toItemStackOrNull() ?: defaultItem) }
            gui.setItem(backwardSlot, ItemBuilder.from(scrollBackward).asGuiItem {
                val index = baseColorScrollingIndex.compute(uniqueId) { _, v -> (v ?: 0) - 1 } ?: 0
                cachedDyeMap.keys.toList().rotatedLeft(index).zip(baseRow).forEach { (item, slot) ->
                    item.setAction {
                        effectToggleState = false
                        val dyeMap = cachedDyeMap[item] ?: return@setAction
                        fillSubColorRow(gui, this, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
                        gui.clearOutputItem()
                    }
                    gui.updateItem(slot, item)
                }
            })
            gui.setItem(forwardSlot, ItemBuilder.from(scrollForward).asGuiItem {
                val index = baseColorScrollingIndex.compute(uniqueId) { _, v -> (v ?: 0) + 1 } ?: 0
                cachedDyeMap.keys.toList().rotatedLeft(index).zip(baseRow).forEach { (item, slot) ->
                    item.setAction {
                        effectToggleState = false
                        val dyeMap = cachedDyeMap[item] ?: return@setAction
                        fillSubColorRow(gui, this, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
                        gui.clearOutputItem()
                    }
                    gui.updateItem(slot, item)
                }
            })
        }
    }

    // Effects toggle
    val effectItem = if (cachedEffectSet.isEmpty() || !hmcColor.config.enableEffectsMenu) null
    else ItemBuilder.from(hmcColor.config.effectItem.toItemStackOrNull() ?: defaultItem).asGuiItem { click ->
        click.isCancelled = true
        effectToggleState = !effectToggleState
        val firstDyeCache = cachedDyeMap.values.firstOrNull() ?: return@asGuiItem
        val effectSubRow = cachedEffectSet.toMutableList()
        // Ensure effectSubRow is same size as firstDyeCache
        // If less, fill start and end of list with GuiItem(AIR) to center whatever is in the effectRowSet
        val middle = (firstDyeCache.size - effectSubRow.size) / 2
        if (effectSubRow.size < firstDyeCache.size) List(middle) { GuiItem(Material.AIR) }.let {
            effectSubRow.addAll(0, it)
            effectSubRow += it
        }
        val dyeMap = if (effectToggleState) effectSubRow else firstDyeCache
        fillSubColorRow(gui, this, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
    }
    effectItem?.let { gui.setItem(buttons.effectButton, it) }

    gui.setDragAction { it.isCancelled = true }
    gui.setOutsideClickAction { it.isCancelled = true }
    gui.setPlayerInventoryAction { click ->
        if (click.isShiftClick) {
            val inputStack = gui.getGuiItem(hmcColor.config.buttons.inputSlot)?.itemStack
            if (inputStack?.isEmpty != false && click.currentItem?.isDyeable() == true) {
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
            }
            // Cancel any non input/output/effectToggle slot
            click.slot !in hmcColor.config.buttons.let { c ->
                setOf(c.inputSlot, c.outputSlot, c.effectButton) }
            -> click.isCancelled = true
            // Cancel adding items to empty output slot
            click.slot == hmcColor.config.buttons.outputSlot && click.currentItem == null -> click.isCancelled = true
            // Cancel everything but leftClick action
            click.slot != hmcColor.config.buttons.outputSlot && click.isShiftClick -> click.isCancelled = true
            // Cancel adding non-dyeable or banned items
            !click.cursor.type.isAir && !click.cursor.isDyeable() -> click.isCancelled = true
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

private fun Gui.clearOutputItem() {
    updateItem(hmcColor.config.buttons.outputSlot, ItemStack(Material.AIR))
}

private fun fillSubColorRow(
    gui: Gui,
    player: Player,
    dyeMap: List<GuiItem>,
    cachedDyeMap: List<GuiItem>,
    cachedEffectSet: Set<GuiItem>
) {
    subColorScrollingIndex[player.uniqueId] = 0 // Reset if player was in before
    val subColorGrid = hmcColor.config.buttons.subColorGrid
    when (subColorGrid.type) {
        HMCColorConfig.SubColorGrid.Type.NORMAL -> {
            subColorGrid.normalGrid.rows.forEachIndexed { rowIndex, subColorRow ->
                // Find the middle of given IntRange
                val middleSubColor = subColorRow.first + subColorRow.count() / 2
                // Subtract 0.1 because we want to round down on .5
                val offset = (dyeMap.size / 2.0 - 0.1).roundToInt()
                val range = max(middleSubColor - offset, subColorRow.first)..min(middleSubColor + offset, subColorRow.last)
                range.forEachIndexed { index, i ->
                    val item  = dyeMap.getOrNull(index + 9 * rowIndex) ?: GuiItem(Material.AIR)
                    item.setAction subAction@{ click ->
                        when {
                            click.isShiftClick -> return@subAction
                            (click.isLeftClick && (item in cachedDyeMap || item in cachedEffectSet)) -> {
                                handleSubColorClick(gui, click, item)
                            }
                        }
                    }
                    gui.updateItem(i, item)
                }
            }
        }
        HMCColorConfig.SubColorGrid.Type.SCROLLING -> {

            fun GuiItem.setSubColorClickAction(click: InventoryClickEvent): Unit? {
                return when {
                    click.isShiftClick -> null
                    (click.isLeftClick && (this in cachedDyeMap || this in cachedEffectSet)) ->
                        handleSubColorClick(gui, click, this)

                    else -> Unit
                }
            }

            val scrollingGrid = hmcColor.config.buttons.subColorGrid.scrollingGrid
            dyeMap.zip(scrollingGrid.row).forEach { (item, slot) ->
                item.setAction { item.setSubColorClickAction(it) }
                gui.updateItem(slot, item)
            }

            val (backwardSlot, scrollBackward) = scrollingGrid.let { it.backwardsSlot to (it.backwardsItem.toItemStackOrNull() ?: defaultItem) }
            val (forwardSlot, scrollForward) = scrollingGrid.let { it.forwardsSlot to (it.forwardsItem.toItemStackOrNull() ?: defaultItem) }

            gui.updateItem(backwardSlot, ItemBuilder.from(scrollBackward).asGuiItem {
                val index = subColorScrollingIndex.compute(player.uniqueId) { _, v -> (v ?: 0) - 1 } ?: 0
                val rotatedDyeMap = dyeMap.rotatedLeft(index).zip(scrollingGrid.row).toMap()
                rotatedDyeMap.forEach { (item, slot) ->
                    item.setAction subAction@{
                        val updatedClickItems = rotatedDyeMap.keys.toList()
                        fillSubColorRow(gui, player, updatedClickItems, updatedClickItems, cachedEffectSet)
                        item.setSubColorClickAction(it)
                    }
                    item.setAction { item.setSubColorClickAction(it) }
                    gui.updateItem(slot, item)
                }
            })

            gui.updateItem(forwardSlot, ItemBuilder.from(scrollForward).asGuiItem {
                val index = subColorScrollingIndex.compute(player.uniqueId) { _, v -> (v ?: 0) + 1 } ?: 0
                val rotatedDyeMap = dyeMap.rotatedLeft(index).zip(scrollingGrid.row).toMap()
                rotatedDyeMap.forEach { (item, slot) ->
                    item.setAction subAction@{ click ->
                        val updatedClickItems = rotatedDyeMap.keys.toList()
                        fillSubColorRow(gui, player, updatedClickItems, updatedClickItems, cachedEffectSet)
                        when {
                            click.isShiftClick -> return@subAction
                            (click.isLeftClick && (item in cachedDyeMap || item in cachedEffectSet)) -> {
                                handleSubColorClick(gui, click, item)
                            }
                        }
                    }
                    item.setAction { item.setSubColorClickAction(it) }
                    gui.updateItem(slot, item)
                }
            })
        }
    }

}

private fun handleSubColorClick(gui: Gui, click: InventoryClickEvent, subColor: GuiItem) {
    val guiInput = click.inventory.getItem(hmcColor.config.buttons.inputSlot)
        ?.let { i -> GuiItem(i) } ?: return
    val guiOutput = GuiItem(guiInput.itemStack.clone())

    guiOutput.itemStack.editItemMeta {
        val appliedColor = (subColor.itemStack.itemMeta?.asColorable())?.color ?: return

        // If player lacks permission, skip applying any color to output item
        hmcColor.config.effects.values.find { e -> e.color == appliedColor }?.let { colors ->
            if (!colors.canUse(click.whoClicked as Player)) return@editItemMeta
        }

        cachedColors.entries.firstOrNull { c -> appliedColor in c.value }?.key?.let { colors ->
            if (!colors.canUse(click.whoClicked as Player)) return@editItemMeta
            if (!click.whoClicked.hasPermission(hmcColor.config.colorPermission)) return@editItemMeta
        }

        (this.asColorable() ?: return).color = appliedColor
        /*if (guiOutput.itemStack.isGearyItem()) {
            val ignores = this.persistentDataContainer.decode<SetItemIgnoredProperties>()?.ignore ?: emptySet()
            this.persistentDataContainer.encode(SetItemIgnoredProperties(ignores.plus(BaseSerializableItemStack.Properties.COLOR)))
        }*/
    }

    gui.updateItem(hmcColor.config.buttons.outputSlot, guiOutput)
    guiOutput.setAction output@{ click ->
        when {
            click.isCancelled -> return@output
            click.cursor.isEmpty && click.currentItem != null -> {
                click.isCancelled = true
                click.currentItem?.editItemMeta {
                    persistentDataContainer.remove(NamespacedKey(hmcColor.plugin, "mf-gui"))
                }?.let {
                    if (!click.isShiftClick) click.whoClicked.setItemOnCursor(it)
                    else click.whoClicked.inventory.addItem(it)
                }

                gui.updateItem(hmcColor.config.buttons.inputSlot, ItemStack.empty())
                gui.updateItem(hmcColor.config.buttons.outputSlot, ItemStack.empty())
                gui.update()
            }
        }
    }
}

internal val noPermissionComponent = Component.text("You do not have access to this color!", NamedTextColor.RED)

fun effectItemList(player: Player) : MutableSet<GuiItem> {
    return hmcColor.config.effects.values.map effectColor@{ effect ->
        GuiItem(defaultItem.editItemMeta {
            displayName(effect.name.miniMsg())
            if (!effect.canUse(player)) lore()?.add(noPermissionComponent) ?: lore(listOf(noPermissionComponent))
            this.asColorable()?.color = effect.color
        })
    }.toMutableSet()
}

fun dyeColorItemMap(player: Player): MutableMap<GuiItem, MutableList<GuiItem>> {
    return mutableMapOf<GuiItem, MutableList<GuiItem>>().apply {
        hmcColor.config.colors.values.forEach baseColor@{ colors ->
            val list = mutableListOf<GuiItem>()
            val baseItem = hmcColor.config.buttons.baseColorGrid.baseColorItem?.toItemStackOrNull((defaultItem)) ?: defaultItem
            val (baseColor, subColors) = colors

            baseItem.editItemMeta {
                displayName(baseColor.name.miniMsg())
                if (!colors.canUse(player)) lore()?.add(noPermissionComponent) ?: lore(listOf(noPermissionComponent))
                this.asColorable()?.color = baseColor.color
            }

            // Make the ItemStacks for all subColors
            val subColorGrid = hmcColor.config.buttons.subColorGrid
            val subItem = hmcColor.config.buttons.subColorGrid.subColorItem?.toItemStackOrNull((defaultItem)) ?: defaultItem
            if (subColors.isEmpty() || subColorGrid.autoFillColorGradient) {
                val centerColor = "#" + baseColor.color.asARGB().toHexString(ColorHelpers.hexFormat).substring(2)
                val count = when (subColorGrid.type) {
                    HMCColorConfig.SubColorGrid.Type.NORMAL -> subColorGrid.normalGrid.rows.flatten()
                    HMCColorConfig.SubColorGrid.Type.SCROLLING -> subColorGrid.scrollingGrid.row
                }.count() + 14
                val gradientComponent = ("<gradient:white:$centerColor:black>" + "X".repeat(count)).miniMsg()

                gradientComponent.children().mapNotNull { it.color()?.takeUnless { c -> c.isCloseToWhite || c.isCloseToBlack } }.forEach {
                    subItem.clone().editItemMeta {
                        displayName(Component.empty())
                        if (!colors.canUse(player)) lore()?.add(noPermissionComponent) ?: lore(listOf(noPermissionComponent))
                        this.asColorable()?.color = Color.fromRGB(it.value())
                    }.let {
                        list += GuiItem(it)
                    }
                }
            } else {
                subColors.forEach subColor@{ subColor ->
                    subItem.editItemMeta {
                        displayName(subColor.name.miniMsg())
                        if (!colors.canUse(player)) lore()?.add(noPermissionComponent) ?: lore(listOf(noPermissionComponent))
                        this.asColorable()?.color = subColor.color
                    }

                    list += GuiItem(subItem)
                }
            }
            if (this.size >= 9) return@baseColor // only show the first 9 baseColors

            this[GuiItem(baseItem)] = list
        }
    }
}

private val TextColor.isCloseToWhite
    get() = red() > 200 && green() > 200 && blue() > 200

private val TextColor.isCloseToBlack
    get() = red() < 50 && green() < 50 && blue() < 50

private val defaultItem
    get() = hmcColor.config.buttons.item.toItemStackOrNull() ?: ItemStack(Material.LEATHER_HORSE_ARMOR)
