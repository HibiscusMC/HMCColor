package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.serialization.IntRangeSerializer
import com.mineinabyss.idofront.serialization.SerializableItemStack
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class HMCColorConfig(
    val title: String,
    val rows: Int = 6,
    val buttons: Buttons,
    val blacklistedItems: BlackListedItems,
    val enableEffectsMenu: Boolean,
    val effectItem: SerializableItemStack,
    val effects: Map<String, Effect>,
    val colorPermission: String = "hmccolor.dye",
    val colors: Map<String, Colors>,
) {

    @Serializable
    data class Buttons(
        val item: SerializableItemStack,
        val inputSlot: Int,
        val outputSlot: Int,
        val baseColorGrid: BaseColorGrid = BaseColorGrid(12..14, 21..23, 30..32),
        val subColorRow: @Serializable(with = IntRangeSerializer::class) IntRange = 46..52,
        val effectButton: Int = 40
    )

    @Serializable
    data class BlackListedItems(
        val oraxenItems: List<String>,
        val crucibleItems: List<String>,
        val itemsadderItems: List<String>,
        val gearyItems: List<String>,
        val types: List<Material>
    )

    @Serializable
    data class Effect(val name: String, val color: String, val permission: String = "")
    @Serializable
    data class Colors(val baseColor: BaseColor, val subColors: Set<SubColor>, val permission: String = "")
    @Serializable
    data class BaseColor(val name: String, val color: String)
    @Serializable
    data class SubColor(val name: String, val color: String)
    @Serializable
    data class BaseColorGrid(
        val first: @Serializable(with = IntRangeSerializer::class) IntRange,
        val second: @Serializable(with = IntRangeSerializer::class) IntRange,
        val third: @Serializable(with = IntRangeSerializer::class) IntRange
    )
}


