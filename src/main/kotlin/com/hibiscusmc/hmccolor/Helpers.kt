package com.hibiscusmc.hmccolor

import com.mineinabyss.geary.papermc.datastore.decodePrefabs
import com.mineinabyss.geary.papermc.tracking.items.gearyItems
import com.mineinabyss.geary.prefabs.PrefabKey
import com.mineinabyss.idofront.items.asColorable
import com.mineinabyss.idofront.plugin.Plugins
import dev.lone.itemsadder.api.CustomStack
import io.lumine.mythiccrucible.MythicCrucible
import io.th0rgal.oraxen.api.OraxenItems
import org.bukkit.inventory.ItemStack

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

fun ItemStack.isDyeable(): Boolean {
    val blacklist = hmcColor.config.blacklistedItems
    return when {
        itemMeta.asColorable() == null -> false
        this.isOraxenItem() -> this.oraxenID() !in blacklist.oraxenItems
        this.isCrucibleItem() -> this.crucibleID() !in blacklist.crucibleItems
        this.isItemsAdderItem() -> this.itemsAdderID() !in blacklist.itemsadderItems
        this.isGearyItem() -> this.gearyID() !in blacklist.gearyItems
        else -> type !in blacklist.types
    }
}