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
import org.bukkit.inventory.meta.PotionMeta
import java.util.logging.Level

fun ItemStack.isOraxenItem() = OraxenItems.exists(this)
fun ItemStack.getOraxenID() = OraxenItems.getIdByItem(this)
fun String.isOraxenItem() = OraxenItems.exists(this)
fun String.getOraxenItem() = OraxenItems.getItemById(this).build()

fun ItemStack.isCrucibleItem() = crucible.itemManager.getItem(this).isPresent
fun ItemStack.getCrucibleId() = crucible.itemManager.getItem(this).get().internalName
fun String.isCrucibleItem() = crucible.itemManager.getItem(this).isPresent
fun String.getCrucibleItem() = MythicCrucible.core().itemManager. getItemStack(this)

fun ItemStack.isItemsAdderItem() = CustomStack.byItemStack(this) != null
fun ItemStack.getItemsAdderID() = CustomStack.byItemStack(this)?.namespacedID
fun String.isItemsAdderItem() = CustomStack.isInRegistry(this)
fun String.getItemsAdderStack() = CustomStack.getInstance(this)?.itemStack

fun ItemStack.isLootyItem() = this.toGearyFromUUIDOrNull() != null
fun ItemStack.getLootyID() = this.toSerializable().prefab
fun String.isLootyItem() = LootyFactory.createFromPrefab(PrefabKey.of(this)) != null
fun String.getLootyItem() = LootyFactory.createFromPrefab(PrefabKey.of(this))

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
    if (itemMeta !is LeatherArmorMeta && itemMeta !is PotionMeta) return false
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
    var effectToggleState: Boolean = false
    val rows = 6
    val gui = Gui.gui(GuiType.CHEST).rows(rows).title(colorConfig.title.miniMsg()).create()

    // baseColor square
    cachedDyeMap.map { it.key }.forEachIndexed { index, guiItem ->
        if (index < 3) gui.setItem(rows - 4, index + 4, guiItem)
        else if (index < 6) gui.setItem(rows - 3, index + 1, guiItem)
        else gui.setItem(rows - 2, index - 2, guiItem)
    }

    // Effects toggle
    val effectItem = if (cachedEffectSet.isNotEmpty()) GuiItem(getDefaultItem()) else null
    effectItem?.let { gui.setItem(rows - 1, 6, it) }

    //TODO Add functionality for when you click the slots etc
    gui.guiItems.forEach { (_, clickedItem) ->
        clickedItem.setAction { click ->
            // Logic for clicking a baseColor to show all subColors
            when {
                click.isShiftClick -> return@setAction//
                click.isLeftClick && (clickedItem in cachedDyeMap.keys || effectItem?.let { it == clickedItem } ?: return@setAction) -> {
                    val dyeMap: List<GuiItem> = when (clickedItem) {
                        effectItem -> {
                            effectToggleState = !effectToggleState
                            if (effectToggleState) cachedEffectSet.toList() else cachedDyeMap.values.firstOrNull()
                                ?: return@setAction
                        }
                        else -> {
                            effectToggleState = false
                            cachedDyeMap[clickedItem] ?: return@setAction
                        }
                    }

                    gui.filler.fillBetweenPoints(rows, 2, rows, 8, dyeMap)

                    (46..52).forEachIndexed { index, i ->
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
                                    val guiInput =
                                        click.inventory.getItem(19)?.let { it1 -> GuiItem(it1) } ?: return@subAction
                                    val guiOutput = GuiItem(guiInput.itemStack.clone())
                                    guiOutput.itemStack.itemMeta = guiOutput.itemStack.itemMeta?.apply {
                                        val appliedColor = subColor.itemStack.itemMeta?.let { meta ->
                                            when (meta) {
                                                is LeatherArmorMeta -> meta.color
                                                is PotionMeta -> meta.color
                                                else -> null
                                            }
                                        } ?: return@subAction

                                        (this as? LeatherArmorMeta)?.setColor(appliedColor)
                                            ?: (this as? PotionMeta)?.setColor(appliedColor) ?: return@apply
                                    }

                                    gui.setItem(25, guiOutput)
                                    gui.updateItem(25, guiOutput)
                                    guiOutput.setAction output@{ click ->
                                        when {
                                            click.cursor?.type == Material.AIR && click.currentItem != null -> {
                                                if (!click.isShiftClick) click.whoClicked.setItemOnCursor(click.currentItem)
                                                else click.whoClicked.inventory.addItem(click.currentItem)
                                                gui.updateItem(20, ItemStack(Material.AIR))
                                                gui.updateItem(25, ItemStack(Material.AIR))
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
    gui.setPlayerInventoryAction { if (it.isShiftClick) it.isCancelled = true }
    gui.setDefaultTopClickAction {
        when {
            it.slot == 19 && it.whoClicked.itemOnCursor.type == Material.AIR -> {
                it.whoClicked.setItemOnCursor(it.inventory.getItem(19))
                gui.updateItem(20, ItemStack(Material.AIR))
                gui.updateItem(25, ItemStack(Material.AIR))
                gui.update()
            }

            it.slot !in setOf(19, 25, 41) -> it.isCancelled = true // Cancel any non input/output/effectToggle slot
            it.slot == 25 && it.currentItem == null -> it.isCancelled = true // Cancel adding items to empty output slot
            it.isShiftClick -> it.isCancelled = true // Cancel everything but leftClick action
            it.cursor?.isDyeable() == false -> it.isCancelled = true // Cancel adding non-dyeable or banned items
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

fun getEffectList(): MutableSet<GuiItem> {
    return colorConfig.effects.map effectColor@{ effect ->
        val effectItem = getDefaultItem()

        effectItem.itemMeta = effectItem.itemMeta?.apply {
            (this as? LeatherArmorMeta)?.setColor(effect.color.toColor())
                ?: (this as? PotionMeta)?.setColor(effect.color.toColor())
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
            (this as? LeatherArmorMeta)?.setColor(baseColor.color.toColor())
                ?: (this as? PotionMeta)?.setColor(baseColor.color.toColor()) ?: return@baseColor
            setDisplayName(baseColor.name.toLegacy())
        } ?: return@baseColor


        // Make the ItemStacks for all subColors
        subColors.forEach subColor@{ color ->
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
                setDisplayName(color.name.toLegacy()) //TODO Make subColor a map and add option for name?
                (this as? LeatherArmorMeta)?.setColor(color.color.toColor())
                    ?: (this as? PotionMeta)?.setColor(color.color.toColor()) ?: return@baseColor
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
