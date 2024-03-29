package com.hibiscusmc.hmccolor

import com.mineinabyss.geary.papermc.GearyPlugin
import com.mineinabyss.geary.papermc.datastore.decodePrefabs
import com.mineinabyss.geary.papermc.tracking.items.gearyItems
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
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
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

fun ItemStack.isGearyItem() =
    this.itemMeta?.persistentDataContainer?.decodePrefabs()?.first()?.let { gearyItems.createItem(it) != null } ?: false

fun ItemStack.getGearyID() = this.itemMeta?.persistentDataContainer?.decodePrefabs()?.first()?.full
fun String.isGearyItem() = PrefabKey.ofOrNull(this)?.let { gearyItems.createItem(it) != null } ?: false
fun String.getGearyItem() = PrefabKey.ofOrNull(this)?.let { gearyItems.createItem(it) }

private fun ItemStack.isDyeable(): Boolean {
    if (itemMeta !is LeatherArmorMeta && itemMeta !is PotionMeta && itemMeta !is MapMeta) return false
    hmcColor.config.blacklistedItems.let { blacklist ->
        return when {
            Plugins.isEnabled<OraxenPlugin>() && this.isOraxenItem() -> this.getOraxenID() !in blacklist.oraxenItems
            Plugins.isEnabled<MythicCrucible>() && this.isCrucibleItem() -> this.getCrucibleId() !in blacklist.crucibleItems
            Plugins.isEnabled("ItemsAdder") && this.isItemsAdderItem() -> this.getItemsAdderID() !in blacklist.itemsadderItems
            Plugins.isEnabled<GearyPlugin>() && this.isGearyItem() -> this.getGearyID() !in blacklist.gearyItems
            else -> type !in blacklist.types
        }
    }
}

fun Player.createColorMenu(): Gui {
    val gui = Gui.gui(GuiType.CHEST).rows(hmcColor.config.rows).title(hmcColor.config.title.miniMsg()).create()
    val cachedDyeMap = dyeColorItemMap(this)
    val cachedEffectSet = effectItemList(this)
    var effectToggleState = false

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
    val effectItem = if (cachedEffectSet.isNotEmpty() && hmcColor.config.enableEffectsMenu) GuiItem(
        hmcColor.config.effectItem.toItemStackOrNull() ?: defaultItem
    ) else null
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
                    val middleSubColor =
                        hmcColor.config.buttons.subColorRow.first + hmcColor.config.buttons.subColorRow.count() / 2
                    // Subtract 0.1 because we want to round down on .5
                    val offset = (dyeMap.size / 2.0 - 0.1).roundToInt()
                    val range = max(
                        middleSubColor - offset,
                        hmcColor.config.buttons.subColorRow.first
                    )..min(middleSubColor + offset, hmcColor.config.buttons.subColorRow.last)
                    range.forEachIndexed { index, i ->
                        gui.updateItem(
                            i,
                            runCatching { // if effect is toggled, we fill based on effect list, otherwise it's a dye color
                                dyeMap[index]
                            }.getOrNull() ?: GuiItem(Material.AIR)
                        )
                        val subColor = gui.getGuiItem(i) ?: return@forEachIndexed
                        subColor.setAction subAction@{
                            when {
                                it.isShiftClick -> return@subAction
                                (click.isLeftClick && (subColor in cachedDyeMap.values.flatten() || subColor in cachedEffectSet)) -> {
                                    val guiInput = click.inventory.getItem(hmcColor.config.buttons.inputSlot)
                                        ?.let { i -> GuiItem(i) } ?: return@subAction
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

                                        // If player lacks permission, skip applying any color to output item
                                        hmcColor.config.effects.values.find { e -> e.color == appliedColor }?.let { colors ->
                                            if (!colors.canUse(click.whoClicked as Player)) return@editItemMeta
                                        }

                                        cachedColors.entries.firstOrNull { c -> appliedColor in c.value }?.key?.let { colors ->
                                            if (!colors.canUse(click.whoClicked as Player)) return@editItemMeta
                                            if (!click.whoClicked.hasPermission(hmcColor.config.colorPermission)) return@editItemMeta
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
                                            click.cursor.type == Material.AIR && click.currentItem != null -> {
                                                click.isCancelled = true
                                                if (!click.isShiftClick) click.whoClicked.setItemOnCursor(click.currentItem)
                                                else click.currentItem?.let { current ->
                                                    click.whoClicked.inventory.addItem(
                                                        current
                                                    )
                                                }
                                                gui.updateItem(
                                                    hmcColor.config.buttons.inputSlot,
                                                    ItemStack(Material.AIR)
                                                )
                                                gui.updateItem(
                                                    hmcColor.config.buttons.outputSlot,
                                                    ItemStack(Material.AIR)
                                                )
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
            if ((inputStack == null || inputStack.type.isAir) && click.currentItem?.isDyeable() == true) {
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

            click.slot !in hmcColor.config.buttons.let { c ->
                setOf(
                    c.inputSlot,
                    c.outputSlot,
                    c.effectButton
                )
            } -> click.isCancelled = true // Cancel any non input/output/effectToggle slot
            click.slot == hmcColor.config.buttons.outputSlot && click.currentItem == null -> click.isCancelled =
                true // Cancel adding items to empty output slot
            click.slot != hmcColor.config.buttons.outputSlot && click.isShiftClick -> click.isCancelled =
                true // Cancel everything but leftClick action
            !click.cursor.type.isAir && !click.cursor.isDyeable() -> click.isCancelled =
                true // Cancel adding non-dyeable or banned items
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

internal val noPermissionComponent = Component.text("You do not have access to this color!", NamedTextColor.RED)

fun effectItemList(player: Player) : MutableSet<GuiItem> {
    return hmcColor.config.effects.values.map effectColor@{ effect ->
        GuiItem(defaultItem.editItemMeta {
            displayName(effect.name.miniMsg())
            if (!effect.canUse(player)) lore()?.add(noPermissionComponent) ?: lore(listOf(noPermissionComponent))
            when (this) {
                is LeatherArmorMeta -> this.setColor(effect.color)
                is PotionMeta -> this.color = effect.color
                is MapMeta -> this.color = effect.color
            }
        })
    }.toMutableSet()
}

fun dyeColorItemMap(player: Player): MutableMap<GuiItem, MutableList<GuiItem>> {
    return mutableMapOf<GuiItem, MutableList<GuiItem>>().apply {
        hmcColor.config.colors.values.forEach baseColor@{ colors ->
            val list = mutableListOf<GuiItem>()
            val baseItem = defaultItem
            val (baseColor, subColors) = colors

            baseItem.editItemMeta {
                displayName(baseColor.name.miniMsg())
                if (!colors.canUse(player)) lore()?.add(noPermissionComponent) ?: lore(listOf(noPermissionComponent))
                when (this) {
                    is LeatherArmorMeta -> this.setColor(baseColor.color)
                    is PotionMeta -> this.color = baseColor.color
                    is MapMeta -> this.color = baseColor.color
                }
            }

            // Make the ItemStacks for all subColors
            subColors.forEach subColor@{ subColor ->
                val subItem = hmcColor.config.buttons.item.toItemStackOrNull() ?: defaultItem

                subItem.editItemMeta {
                    displayName(subColor.name.miniMsg())
                    if (!colors.canUse(player)) lore()?.add(noPermissionComponent) ?: lore(listOf(noPermissionComponent))
                    when (this) {
                        is LeatherArmorMeta -> this.setColor(subColor.color)
                        is PotionMeta -> this.color = subColor.color
                        is MapMeta -> this.color = subColor.color
                    }
                }

                if (list.size >= 7) return@subColor // Only allow for 7 subColor options
                list += GuiItem(subItem)
            }
            if (this.size >= 9) return@baseColor // only show the first 9 baseColors

            this[GuiItem(baseItem)] = list
        }
    }
}


private val defaultItem
    get() = hmcColor.config.buttons.item.toItemStackOrNull() ?: ItemStack(Material.LEATHER_HORSE_ARMOR)
