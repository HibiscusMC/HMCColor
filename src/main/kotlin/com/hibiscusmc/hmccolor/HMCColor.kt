package com.hibiscusmc.hmccolor

import com.mineinabyss.geary.papermc.tracking.items.gearyItems
import com.mineinabyss.geary.prefabs.PrefabKey
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.di.DI
import com.mineinabyss.idofront.serialization.SerializablePrefabItemService
import dev.triumphteam.gui.guis.Gui
import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

var cachedColors = mutableMapOf<HMCColorConfig.Colors, Set<Color>>()
class HMCColor : JavaPlugin() {
    override fun onEnable() {
        createColorContext()
        HMCColorCommands()
    }

    override fun onDisable() {
        server.onlinePlayers.filter { it.openInventory.topInventory.holder is Gui }.forEach(Player::closeInventory)
    }

    fun createColorContext() {
        val config = runCatching { config("config", dataFolder.toPath(), HMCColorConfig()).getOrLoad() }
            .onFailure {
                logger.severe("Failed to load HMCColor config: " + it.message)
                logger.severe("Loading default config...")
            }.getOrDefault(HMCColorConfig())
        DI.remove<HMCColorContext>()
        val colorContext = object : HMCColorContext {
            override val plugin = this@HMCColor
            override val config = config
            override val helpers = ColorHelpers()
        }
        DI.add<HMCColorContext>(colorContext)

        DI.add<SerializablePrefabItemService>(
            object : SerializablePrefabItemService {
                override fun encodeFromPrefab(item: ItemStack, prefabName: String) {
                    val result = gearyItems.createItem(PrefabKey.of(prefabName), item)
                    require(result != null) { "Failed to create serializable ItemStack from $prefabName, does the prefab exist and have a geary:set.item component?" }
                }
            })

        cachedColors = hmcColor.config.colors.values.associateWith { it.allColors }.toMutableMap()
    }
}
