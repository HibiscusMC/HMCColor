package com.hibiscusmc.hmccolor

import dev.lone.itemsadder.api.CustomStack
import dev.lone.itemsadder.api.ItemsAdder
import io.th0rgal.oraxen.items.OraxenItems
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

fun ItemStack.isOraxenItem() = OraxenItems.getIdByItem(this) != null
fun ItemStack.getOraxenID() = OraxenItems.getIdByItem(this)

fun ItemStack.isItemsAdderItem() = CustomStack.byItemStack(this) != null
fun ItemStack.getItemsAdderStack() = CustomStack.byItemStack(this)

fun String.miniMsg() = MiniMessage.miniMessage().deserialize(this)

fun Any.broadcastVal(prefix: String = "") = Bukkit.broadcastMessage(prefix + this.toString())

fun ItemStack.setCustomModelData(int: Int) : ItemStack {
    val meta = itemMeta
    meta?.setCustomModelData(int)
    this.itemMeta = meta
    return this
}
