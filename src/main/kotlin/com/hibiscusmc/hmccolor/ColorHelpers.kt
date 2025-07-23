package com.hibiscusmc.hmccolor

import com.hibiscusmc.hmccolor.extensions.Version
import com.hibiscusmc.hmccolor.extensions.deserialize
import com.hibiscusmc.hmccolor.extensions.rotatedLeft
import com.mineinabyss.idofront.items.asColorable
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.serialization.SerializableItemStack
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ColorHelpers {

    private val baseColorScrollingIndex: MutableMap<UUID, Int> = mutableMapOf()
    private val subColorScrollingIndex: MutableMap<UUID, Int> = mutableMapOf()

    fun createColorMenus(player: Player): Gui {
        val gui = Gui.gui().rows(hmcColor.config.rows).title(hmcColor.config.title.deserialize()).inventory { title, owner, rows ->
            Bukkit.createInventory(owner, rows, title)
        }.create()
        val cachedDyeMap = dyeColorItemMap(player)
        val cachedEffectSet = effectItemList(player)
        var effectToggleState = false

        val buttons = hmcColor.config.buttons
        val baseColorGrid = buttons.baseColorGrid

        baseColorScrollingIndex[player.uniqueId] = 0 // Reset if player was in before
        subColorScrollingIndex[player.uniqueId] = 0 // Reset if player was in before

        when (baseColorGrid.type) {
            HMCColorConfig.BaseColorGrid.Type.NORMAL -> baseColorGrid.normalGrid.rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { index, int ->
                    gui.setItem(int, cachedDyeMap.keys.elementAt(index + 3 * rowIndex).also { item ->
                        item.setAction {
                            effectToggleState = false
                            val dyeMap = cachedDyeMap[item] ?: return@setAction
                            fillSubColorRow(gui, player, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
                            gui.clearOutputItem()
                        }
                    })
                }
            }
            HMCColorConfig.BaseColorGrid.Type.SCROLLING -> {
                val cachedDyeKeys = cachedDyeMap.keys.toList()
                val baseRow = baseColorGrid.scrollingGrid.row
                val baseRowIndices = baseRow.toList()
                val baseRowCenterSlot = baseRow.last - ((baseRowIndices.size - 1) / 2)

                fun centerClickedBaseColor(clickedItem: GuiItem, clickedSlot: Int) {
                    val centerOffset = clickedSlot - baseRowCenterSlot
                    val newIndex = (baseColorScrollingIndex[player.uniqueId] ?: 0) + centerOffset
                    baseColorScrollingIndex[player.uniqueId] = newIndex
                    cachedDyeKeys.rotatedLeft(newIndex).zip(baseRow).forEach {(item, slot) ->
                        item.setAction {
                            centerClickedBaseColor(clickedItem, slot)
                            effectToggleState = false
                            val dyeMap = cachedDyeMap[item] ?: return@setAction
                            fillSubColorRow(gui, player, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
                            gui.clearOutputItem()
                        }
                        gui.updateItem(slot, item)
                    }
                }

                fun selectBaseColor(item: GuiItem, slot: Int) {
                    centerClickedBaseColor(item, slot)
                    effectToggleState = false
                    val dyeMap = cachedDyeMap[item] ?: return
                    fillSubColorRow(gui, player, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
                    gui.clearOutputItem()
                }

                // Initial setup of base color row
                cachedDyeKeys.zip(baseRow).forEach { (item, slot) ->
                    item.setAction { selectBaseColor(item, slot) }
                    gui.updateItem(slot, item)
                }

                val (backwardSlot, scrollBackward) = baseColorGrid.scrollingGrid.let { it.backwardSlot to (it.backwardItem.toItemStackOrDefaultItem()) }
                val (forwardSlot, scrollForward) = baseColorGrid.scrollingGrid.let { it.forwardSlot to (it.forwardItem.toItemStackOrDefaultItem()) }
                gui.updateItem(backwardSlot, ItemBuilder.from(scrollBackward).asGuiItem {
                    val index = baseColorScrollingIndex.compute(player.uniqueId) { _, v -> (v ?: 0) - 1 } ?: 0
                    cachedDyeKeys.rotatedLeft(index).zip(baseRow).forEach { (item, slot) ->
                        item.setAction { selectBaseColor(item, slot) }
                        gui.updateItem(slot, item)
                    }
                    val centerBaseColor = gui.getGuiItem(baseRowCenterSlot) ?: return@asGuiItem
                    selectBaseColor(centerBaseColor, baseRowCenterSlot)
                })
                gui.updateItem(forwardSlot, ItemBuilder.from(scrollForward).asGuiItem {
                    val index = baseColorScrollingIndex.compute(player.uniqueId) { _, v -> (v ?: 0) + 1 } ?: 0
                    cachedDyeKeys.rotatedLeft(index).zip(baseRow).forEach { (item, slot) ->
                        item.setAction { selectBaseColor(item, slot) }
                        gui.updateItem(slot, item)
                    }
                    val centerBaseColor = gui.getGuiItem(baseRowCenterSlot) ?: return@asGuiItem
                    selectBaseColor(centerBaseColor, baseRowCenterSlot)
                })
            }
        }

        // Effects toggle
        val effectItem = if (cachedEffectSet.isEmpty() || !hmcColor.config.enableEffectsMenu) null
        else ItemBuilder.from(hmcColor.config.effectItem.toItemStackOrDefaultItem()).asGuiItem { click ->
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
            fillSubColorRow(gui, player, dyeMap, cachedDyeMap.values.flatten(), cachedEffectSet)
        }
        effectItem?.let { gui.setItem(buttons.effectButton, it) }

        val closeMenuItem = hmcColor.config.closeButton.toItemStackOrDefaultItem()
            .takeIf { buttons.closeButton != null }?.let(ItemBuilder::from)?.asGuiItem { click ->
                click.isCancelled = true
                click.whoClicked.closeInventory(InventoryCloseEvent.Reason.PLUGIN)
            }

        closeMenuItem?.let { gui.setItem(buttons.closeButton!!, it) }

        gui.setDragAction { it.isCancelled = true }
        gui.setOutsideClickAction { it.isCancelled = true }
        gui.setPlayerInventoryAction { click ->
            if (click.isShiftClick) {
                val inputStack = gui.getGuiItem(hmcColor.config.buttons.inputSlot)?.itemStack
                if (inputStack?.type?.isAir != false && click.currentItem?.isDyeable() == true) {
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
                click.slot == hmcColor.config.buttons.outputSlot && (click.currentItem == null || !click.cursor.type.isAir) -> click.isCancelled = true
                // Cancel everything but leftClick action
                click.slot != hmcColor.config.buttons.outputSlot && click.isShiftClick -> click.isCancelled = true
                // Cancel adding non-dyeable or banned items
                !click.cursor.type.isAir -> when {
                    !click.cursor.isDyeable() -> click.isCancelled = true
                    !click.cursor.isSimilar(click.currentItem) -> gui.clearOutputItem()
                }
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

    private fun clearSubColorRows(gui: Gui) {
        when (hmcColor.config.buttons.subColorGrid.type) {
            HMCColorConfig.SubColorGrid.Type.NORMAL -> {
                hmcColor.config.buttons.subColorGrid.normalGrid.rows.flatten().forEach {
                    gui.updateItem(it, GuiItem(Material.AIR))
                }
            }
            HMCColorConfig.SubColorGrid.Type.SCROLLING -> {
                hmcColor.config.buttons.subColorGrid.scrollingGrid.row.forEach {
                    gui.updateItem(it, GuiItem(Material.AIR))
                }
            }
        }
    }

    private fun fillSubColorRow(
        gui: Gui,
        player: Player,
        dyeMap: List<GuiItem>,
        cachedDyeMap: List<GuiItem>,
        cachedEffectSet: Set<GuiItem>
    ) {
        clearSubColorRows(gui)
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
                val subRow = scrollingGrid.row.toList()
                val subRowCenterSlot = scrollingGrid.row.last - ((subRow.size - 1) / 2)

                fun centerClickedSubColor(clickedItem: GuiItem, clickedSlot: Int) {
                    val centerOffset = clickedSlot - subRowCenterSlot
                    val newIndex = (subColorScrollingIndex[player.uniqueId] ?: 0) + centerOffset
                    subColorScrollingIndex[player.uniqueId] = newIndex
                    dyeMap.rotatedLeft(newIndex).zip(scrollingGrid.row).forEach { (item, slot) ->
                        item.setAction {
                            centerClickedSubColor(clickedItem, slot)
                            item.setSubColorClickAction(it)
                        }
                        gui.updateItem(slot, item)
                    }
                }

                // Initial setup for sub color row
                dyeMap.zip(scrollingGrid.row).forEach { (item, slot) ->
                    item.setAction {
                        centerClickedSubColor(item, slot)
                        item.setSubColorClickAction(it)
                    }
                    gui.updateItem(slot, item)
                }

                val (backwardSlot, scrollBackward) = scrollingGrid.let { it.backwardsSlot to (it.backwardsItem.toItemStackOrDefaultItem()) }
                val (forwardSlot, scrollForward) = scrollingGrid.let { it.forwardsSlot to (it.forwardsItem.toItemStackOrDefaultItem()) }

                gui.updateItem(backwardSlot, ItemBuilder.from(scrollBackward).asGuiItem {
                    val index = subColorScrollingIndex.compute(player.uniqueId) { _, v -> (v ?: 0) - 1 } ?: 0
                    val rotatedDyeMap = dyeMap.rotatedLeft(index).zip(scrollingGrid.row).toMap()
                    rotatedDyeMap.forEach { (item, slot) ->
                        item.setAction subAction@{
                            val updatedClickItems = rotatedDyeMap.keys.toList()
                            fillSubColorRow(gui, player, updatedClickItems, updatedClickItems, cachedEffectSet)
                            centerClickedSubColor(item, slot)
                            item.setSubColorClickAction(it)
                        }
                        item.setAction {
                            centerClickedSubColor(item, slot)
                            item.setSubColorClickAction(it)
                        }
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

    private fun handleSubColorClick(gui: Gui, click: InventoryClickEvent, subColorItem: GuiItem) {
        val guiInput = click.inventory.getItem(hmcColor.config.buttons.inputSlot)?.let { i -> GuiItem(i) } ?: return
        val guiOutput = GuiItem(
            guiInput.itemStack.clone().also { itemStack ->
                val appliedColor = when {
                    Version.atleast("1.21.4") -> subColorItem.itemStack.asColorable()
                    else -> subColorItem.itemStack.itemMeta?.asColorable()
                }?.color ?: return@also
                // If player lacks permission, skip applying any color to output item
                hmcColor.config.effects.values.firstOrNull { e -> e.color == appliedColor }?.let { colors ->
                    if (!colors.canUse(click.whoClicked as Player)) return@also
                }

                hmcColor.config.colors.values.map { it.subColors }.flatten().find { it.color == appliedColor }?.let { subColor ->
                    val player = click.whoClicked as? Player ?: return@let
                    val baseColor = hmcColor.config.colors.values.find { subColor in it.subColors }?.baseColor ?: return@also
                    if (!subColor.canUse(player, baseColor)) return@also
                }

                when {
                    Version.atleast("1.21.4") -> (itemStack.asColorable() ?: return)
                    else -> (itemStack.itemMeta?.asColorable() ?: return)
                }.color = appliedColor

            }
        )

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

    private fun effectItemList(player: Player) : MutableSet<GuiItem> {
        return hmcColor.config.effects.values.map effectColor@{ effect ->
            GuiItem(defaultItem.editItemMeta {
                displayName(effect.name.deserialize())
                if (!effect.canUse(player)) lore()?.add(hmcColor.config.noPermissionComponent) ?: lore(listOf(hmcColor.config.noPermissionComponent))
                this.asColorable()?.color = effect.color
            })
        }.toMutableSet()
    }

    private fun dyeColorItemMap(player: Player): MutableMap<GuiItem, MutableList<GuiItem>> {
        return mutableMapOf<GuiItem, MutableList<GuiItem>>().apply {
            hmcColor.config.colors.values.forEach baseColor@{ colors ->
                val list = mutableListOf<GuiItem>()
                val baseItem = hmcColor.config.buttons.baseColorGrid.baseColorItem?.toItemStackOrNull((defaultItem)) ?: defaultItem
                val (baseColor, subColors) = colors

                baseItem.editItemMeta {
                    displayName(baseColor.name.deserialize())
                    if (!baseColor.canUse(player)) lore()?.add(hmcColor.config.noPermissionComponent) ?: lore(listOf(hmcColor.config.noPermissionComponent))
                    this.asColorable()?.color = baseColor.color
                }

                // Make the ItemStacks for all subColors
                val subColorGrid = hmcColor.config.buttons.subColorGrid
                val subItem = hmcColor.config.buttons.subColorGrid.subColorItem?.toItemStackOrNull((defaultItem)) ?: defaultItem
                if (subColors.isEmpty() || subColorGrid.autoFillColorGradient) {
                    val count = when (subColorGrid.type) {
                        HMCColorConfig.SubColorGrid.Type.NORMAL -> subColorGrid.normalGrid.rows.flatten().count() * 2
                        HMCColorConfig.SubColorGrid.Type.SCROLLING -> subColorGrid.scrollingGrid.row.let { it.last - it.first } * 2
                    }
                    val hueGradient = createGradientWithHueShift(Color(baseColor.color.asRGB()), count)

                    hueGradient.forEach { color: org.bukkit.Color ->
                        subItem.clone().editItemMeta {
                            displayName(Component.empty())
                            if (!baseColor.canUse(player)) lore()?.add(hmcColor.config.noPermissionComponent) ?: lore(listOf(hmcColor.config.noPermissionComponent))
                            this.asColorable()?.color = color
                        }.let {
                            list += GuiItem(it)
                        }
                    }
                    cachedColors.compute(colors) { _, allColors ->
                        (allColors ?: setOf()).plus(hueGradient)
                    }

                } else {
                    subColors.forEach subColor@{ subColor ->
                        subItem.clone().editItemMeta {
                            displayName(subColor.name.deserialize())
                            if (!subColor.canUse(player, baseColor)) lore()?.add(hmcColor.config.noPermissionComponent) ?: lore(listOf(hmcColor.config.noPermissionComponent))
                            this.asColorable()?.color = subColor.color
                        }.let {
                            list += GuiItem(it)
                        }
                    }
                }

                this[GuiItem(baseItem)] = list
            }
        }
    }

    private fun createGradientWithHueShift(primaryColor: Color, numSteps: Int): List<org.bukkit.Color> {
        val gradients = Array(numSteps) { Color(0, 0, 0) }

        // Convert primary color to HSB
        val hsb = Color.RGBtoHSB(primaryColor.red, primaryColor.green, primaryColor.blue, null)
        val hue = hsb[0]
        val saturation = hsb[1]
        val brightness = hsb[2]

        // Determine extreme "light" and "dark" colors based on brightness
        val lightColor = Color.getHSBColor(hue, saturation, Math.min(brightness * 1.5f, 1.0f))
        val darkColor = Color.getHSBColor(hue, saturation, Math.max(brightness * 0.5f, 0.0f))

        // Calculate color difference between light and dark colors
        val lightHSB = Color.RGBtoHSB(lightColor.red, lightColor.green, lightColor.blue, null)
        val darkHSB = Color.RGBtoHSB(darkColor.red, darkColor.green, darkColor.blue, null)

        val hueDiff = (lightHSB[0] - darkHSB[0]) / numSteps
        val saturationDiff = (lightHSB[1] - darkHSB[1]) / numSteps
        val brightnessDiff = (lightHSB[2] - darkHSB[2]) / numSteps

        // Generate gradient colors
        for (step in 0 until numSteps) {
            val newHue = lightHSB[0] - hueDiff * step
            val newSaturation = lightHSB[1] - saturationDiff * step
            val newBrightness = lightHSB[2] - brightnessDiff * step

            gradients[step] = Color.getHSBColor(newHue, newSaturation, newBrightness)
        }

        return gradients.map { org.bukkit.Color.fromRGB(it.red.coerceIn(0, 255), it.green.coerceIn(0, 255), it.blue.coerceIn(0, 255)) }
    }

    internal val defaultItem by lazy { hmcColor.config.buttons.item.toItemStackOrNull() ?: ItemStack(Material.LEATHER_HORSE_ARMOR) }

}

private fun SerializableItemStack.toItemStackOrDefaultItem() = toItemStackOrNull() ?: hmcColor.helpers.defaultItem
