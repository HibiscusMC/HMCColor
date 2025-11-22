package com.hibiscusmc.hmccolor

import com.mineinabyss.geary.papermc.datastore.decodePrefabs
import com.mineinabyss.geary.papermc.gearyPaper
import com.mineinabyss.geary.papermc.tracking.items.ItemTracking
import com.mineinabyss.geary.prefabs.PrefabKey
import com.mineinabyss.idofront.items.asColorable
import com.mineinabyss.idofront.plugin.Plugins
import com.nexomc.nexo.api.NexoItems
import dev.lone.itemsadder.api.CustomStack
import io.lumine.mythiccrucible.MythicCrucible
import org.bukkit.inventory.ItemStack

fun ItemStack.isNexoItem() = Plugins.isEnabled("Nexo") && NexoItems.exists(this)
fun ItemStack.nexoID(): String? = if (Plugins.isEnabled("Nexo")) NexoItems.idFromItem(this) else null
fun String.isNexoItem() = Plugins.isEnabled("Nexo") && NexoItems.exists(this)
fun String.nexoItem(): ItemStack? = if (Plugins.isEnabled("Nexo")) NexoItems.itemFromId(this)?.build() else null

fun ItemStack.isCrucibleItem() = Plugins.isEnabled("MythicCrucible") && MythicCrucible.inst()?.itemManager?.getItem(this)?.isPresent ?: false
fun ItemStack.crucibleID(): String? = if (Plugins.isEnabled("MythicCrucible")) MythicCrucible.inst()?.itemManager?.getItem(this)?.get()?.internalName else null
fun String.isCrucibleItem() = Plugins.isEnabled("MythicCrucible") && MythicCrucible.inst()?.itemManager?.getItem(this)?.isPresent ?: false
fun String.crucibleItem(): ItemStack? = if (Plugins.isEnabled("MythicCrucible")) MythicCrucible.core().itemManager.getItemStack(this) else null

fun ItemStack.isItemsAdderItem() = Plugins.isEnabled("ItemsAdder") && CustomStack.byItemStack(this) != null
fun ItemStack.itemsAdderID() = if (Plugins.isEnabled("ItemsAdder")) CustomStack.byItemStack(this)?.namespacedID else null
fun String.isItemsAdderItem() = Plugins.isEnabled("ItemsAdder") && CustomStack.isInRegistry(this)
fun String.itemsAdderItem() = if (Plugins.isEnabled("ItemsAdder")) CustomStack.getInstance(this)?.itemStack else null

val gearyItems get() = gearyPaper.worldManager.global.getAddon(ItemTracking)
val globalGeary get() = gearyPaper.worldManager.global
fun ItemStack.isGearyItem() = Plugins.isEnabled("Geary") && with(gearyPaper.worldManager.global) { this@isGearyItem.itemMeta?.persistentDataContainer?.decodePrefabs()?.firstOrNull()?.let { gearyItems.createItem(it) != null } ?: false }
fun ItemStack.gearyID() = with(gearyPaper.worldManager.global) { this@gearyID.itemMeta?.persistentDataContainer?.decodePrefabs()?.first()?.full }
fun String.isGearyItem() = Plugins.isEnabled("Geary") && PrefabKey.ofOrNull(this)?.let { gearyItems.createItem(it) != null } ?: false
fun String.gearyItem() = if (Plugins.isEnabled("Geary")) PrefabKey.ofOrNull(this)?.let { gearyItems.createItem(it) } else null

fun ItemStack.isDyeable(): Boolean {
    val blacklist = hmcColor.config.blacklistedItems
    return when {
        runCatching { asColorable() }.getOrDefault(itemMeta.asColorable()) == null -> false
        this.isNexoItem() -> this.nexoID() !in blacklist.nexoItems
        this.isCrucibleItem() -> this.crucibleID() !in blacklist.crucibleItems
        this.isItemsAdderItem() -> this.itemsAdderID() !in blacklist.itemsadderItems
        this.isGearyItem() -> this.gearyID() !in blacklist.gearyItems
        else -> type !in blacklist.types
    }
}