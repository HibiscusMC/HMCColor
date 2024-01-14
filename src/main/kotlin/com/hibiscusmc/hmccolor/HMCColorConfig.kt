package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.serialization.IntRangeSerializer
import com.mineinabyss.idofront.serialization.SerializableItemStack
import com.mineinabyss.idofront.util.toColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission

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
    data class Effect(val name: String, @SerialName("color") private val _color: String, @SerialName("permission") private val _permission: String = "") {
        @Transient val color = _color.toColor()
        @Transient val permission = Permission(_permission)
    }

    @Serializable
    data class Colors(val baseColor: BaseColor, val subColors: Set<SubColor>, @SerialName("permission") internal val _permission: String = "") {
        @Transient val allColors = setOf(baseColor.color) + subColors.map(SubColor::color).toSet()
        @Transient val permission: Permission = Permission(_permission)
        operator fun Colors.component3() = permission

        @Serializable
        data class BaseColor(val name: String, @SerialName("color") private val _color: String) {
            @Transient val color = _color.toColor()
        }

        @Serializable
        data class SubColor(val name: String, @SerialName("color") private val _color: String) {
            @Transient val color = _color.toColor()
        }
    }

    @Serializable
    data class BaseColorGrid(
        val first: @Serializable(with = IntRangeSerializer::class) IntRange,
        val second: @Serializable(with = IntRangeSerializer::class) IntRange,
        val third: @Serializable(with = IntRangeSerializer::class) IntRange
    )
}


