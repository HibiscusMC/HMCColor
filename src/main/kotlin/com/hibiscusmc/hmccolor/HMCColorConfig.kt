@file:OptIn(ExperimentalSerializationApi::class)

package com.hibiscusmc.hmccolor

import com.charleskorn.kaml.YamlComment
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.serialization.ColorSerializer
import com.mineinabyss.idofront.serialization.IntRangeSerializer
import com.mineinabyss.idofront.serialization.MaterialByNameSerializer
import com.mineinabyss.idofront.serialization.SerializableItemStack
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.util.toColor
import kotlinx.serialization.*
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission

@Serializable
data class HMCColorConfig(
    val title: String = "<gradient:#aa3159:#8c0c0c>HMCColor",
    val rows: Int = 6,
    val buttons: Buttons = Buttons(),
    val blacklistedItems: BlackListedItems = BlackListedItems(),
    val enableEffectsMenu: Boolean = false,
    @YamlComment(
        "Can also be: ",
        "oraxenItem: something",
        "crucibleItem: something",
        "itemsadderItem: hmccosmetics:something",
        "prefab: namespace:something"
    )
    val effectItem: SerializableItemStack = SerializableItemStack(
        type = Material.LEATHER_HORSE_ARMOR,
        displayName = "Effect Toggle".miniMsg().noItalic(),
        color = "#FFFCFC".toColor(),
        customModelData = 11
    ),
    val effects: Map<String, Effect> = mapOf(
        "space_effect" to Effect(
            "Space Effect",
            "#FCFC00".toColor(),
            "hmccolor.effect.space"
        )
    ),
    val colorPermission: String = "hmccolor.dye",
    val colors: Map<String, Colors> = defaultColors
) {

    @Serializable
    data class Buttons(
        @YamlComment(
            "Can also be: ",
            "oraxenItem: paintbrush",
            "crucibleItem: something",
            "itemsadderItem: hmccosmetics:paintbrush",
            "prefab: namespace:something"
        )
        val item: SerializableItemStack = SerializableItemStack(
            type = Material.LEATHER_HORSE_ARMOR,
            customModelData = 1
        ),
        val inputSlot: Int = 19,
        val outputSlot: Int = 25,
        val baseColorGrid: BaseColorGrid = BaseColorGrid(),
        val subColorGrid: SubColorGrid = SubColorGrid(),
        val effectButton: Int = 40,
    )

    @Serializable
    data class BlackListedItems(
        val oraxenItems: List<String> = listOf("banned_oraxen_id"),
        val crucibleItems: List<String> = listOf("banned_crucible_id"),
        val itemsadderItems: List<String> = listOf("banned_itemsadder_id"),
        val gearyItems: List<String> = listOf("banned_geary_id"),
        val types: List<@Serializable(MaterialByNameSerializer::class) Material> = listOf(Material.LEATHER_CHESTPLATE)
    )

    @Serializable
    data class Effect(
        val name: String,
        val color: @Serializable(ColorSerializer::class) Color,
        @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("permission") private val _permission: String? = null
    ) {
        @Transient
        val permission = _permission.takeUnless { it.isNullOrEmpty() }?.let { Permission(it) }
        fun canUse(player: Player) = permission?.let { player.hasPermission(it) } ?: true
    }

    @Serializable
    data class Colors(
        val baseColor: BaseColor,
        val subColors: Set<SubColor>,
    ) {
        @Transient
        val allColors = setOf(baseColor.color).plus(subColors.map { it.color })

        @Serializable
        data class BaseColor(val name: String, val color: @Serializable(ColorSerializer::class) Color, @EncodeDefault(EncodeDefault.Mode.NEVER) val permission: String? = null) {
            fun canUse(player: Player): Boolean {
                return permission.takeUnless { it.isNullOrEmpty() }?.let { player.hasPermission(it) } ?: true
            }
        }

        @Serializable
        data class SubColor(val name: String, val color: @Serializable(ColorSerializer::class) Color, @EncodeDefault(EncodeDefault.Mode.NEVER) val permission: String? = null) {
            fun canUse(player: Player, baseColor: BaseColor): Boolean {
                return baseColor.canUse(player) && (permission.takeUnless { it.isNullOrEmpty() }?.let { player.hasPermission(it) } ?: true)
            }
        }
    }

    @Serializable
    data class BaseColorGrid(
        @YamlComment("Valid types are: NORMAL, SCROLLING")
        val type: Type = Type.NORMAL,
        val normalGrid: Normal = Normal(),
        val scrollingGrid: Scrolling = Scrolling(),
        @YamlComment("baseColorItem can be specified, null defaults to Buttons.item")
        val baseColorItem: SerializableItemStack? = null
    ) {

        @Serializable
        data class Normal(
            val first: @Serializable(with = IntRangeSerializer::class) IntRange = 12..14,
            val second: @Serializable(with = IntRangeSerializer::class) IntRange = 21..23,
            val third: @Serializable(with = IntRangeSerializer::class) IntRange = 30..32
        ) {
            val rows get() = listOf(first, second, third)
        }

        @Serializable
        data class Scrolling(
            val row: @Serializable(with = IntRangeSerializer::class) IntRange = 19..25,
            val backwardSlot: Int = row.first.minus(1),
            val forwardSlot: Int = row.last.plus(1),
            val backwardItem: SerializableItemStack = scrollBackwardDefault.copy(displayName = "Scroll base-colors backwards".miniMsg()),
            val forwardItem: SerializableItemStack = scrolForwardDefault.copy(displayName = "Scroll base-colors forward".miniMsg())
        )

        enum class Type {
            NORMAL, SCROLLING
        }
    }

    @Serializable
    data class SubColorGrid(
        @YamlComment("Valid types are: NORMAL, SCROLLING")
        val type: Type = Type.NORMAL,
        @YamlComment("Whether to ignore the listed subcolors and instead use a gradient from the white->baseColor->black.")
        val autoFillColorGradient: Boolean = true,
        val normalGrid: Normal = Normal(),
        val scrollingGrid: Scrolling = Scrolling(),
        @YamlComment("subColorItem can be specified, null defaults to Buttons.item")
        val subColorItem: SerializableItemStack? = null
    ) {

        @Serializable
        data class Normal(
            val rows: List<@Serializable(with = IntRangeSerializer::class) IntRange> = listOf(37..43, 46..52)
        )

        @Serializable
        data class Scrolling(
            val row: @Serializable(with = IntRangeSerializer::class) IntRange = 46..52,
            val backwardsSlot: Int = 36,
            val forwardsSlot: Int = 45,
            val backwardsItem: SerializableItemStack = scrollBackwardDefault.copy(displayName = "Scroll sub-colors backwards".miniMsg().noItalic()),
            val forwardsItem: SerializableItemStack = scrolForwardDefault.copy(displayName = "Scroll sub-colors forward".miniMsg().noItalic())
        )
        enum class Type {
            NORMAL, SCROLLING
        }
    }

    companion object {
        private val scrolForwardDefault = SerializableItemStack(
            type = Material.ARROW,
            displayName = Component.text("Next Page"),
            customModelData = 2
        )

        private val scrollBackwardDefault = SerializableItemStack(
            type = Material.ARROW,
            displayName = Component.text("Previous Page"),
            customModelData = 1
        )

        private val defaultColors = mapOf(
            "red" to Colors(
                Colors.BaseColor("<#D23635>Red", "#D23635".toColor()),
                setOf(
                    Colors.SubColor("<#FF0000>Light Red", "#FF0000".toColor()),
                    Colors.SubColor("<#CE2029>Fire Engine Red", "#01CE2029".toColor()),
                    Colors.SubColor("<#66023C>Tyrian Red", "#FF66023C".toColor()),
                    Colors.SubColor("<#C71585>Red Violet", "#C71585".toColor()),
                    Colors.SubColor("<#B3446C>Raspberry Red", "#B3446C".toColor()),
                    Colors.SubColor("<#DE3163>Cerise Red", "#DE3163".toColor()),
                    Colors.SubColor("<#FF6961>Neon Red", "#FF6961".toColor())
                )
            ),
            "orange" to Colors(
                Colors.BaseColor("<#EA5C2B>Orange", "#EA5C2B".toColor()),
                setOf(
                    Colors.SubColor("<#FF9f00>Orange Peel", "#FF9F00".toColor()),
                    Colors.SubColor("<#FFA500>Light Orange", "#FFA500".toColor()),
                    Colors.SubColor("<#F28500>Tangerine Orange", "#F28500".toColor()),
                    Colors.SubColor("<#FFB347>Pastel Orange", "#FFB347".toColor()),
                    Colors.SubColor("<#E24C00>Bright Orange", "#E24C00".toColor()),
                    Colors.SubColor("<#EC5800>Persimmon Orange", "#EC5800".toColor()),
                    Colors.SubColor("<#FFA500>Orange", "#FFA500".toColor())
                )
            ),
            "blue" to Colors(
                Colors.BaseColor("<#3731B5>Blue", "#3731B5".toColor()),
                setOf(
                    Colors.SubColor("<#318CE7>France Blue", "#318CE7".toColor()),
                    Colors.SubColor("<#034694>Chelsea Blue", "#034694".toColor()),
                    Colors.SubColor("<#007FFF>Azure Blue", "#007FFF".toColor()),
                    Colors.SubColor("<#0000FF>Blue", "#0000FF".toColor()),
                    Colors.SubColor("<#13274F>Braves Navy", "#13274F".toColor()),
                    Colors.SubColor("<#00008B>Dark Blue", "#00008B".toColor()),
                    Colors.SubColor("<#6495ED>Cornflower Blue", "#6495ED".toColor())
                )
            ),
            "light_blue" to Colors(
                Colors.BaseColor("<#32ECE8>Light Blue", "#32ECE8".toColor()),
                setOf(
                    Colors.SubColor("<#0CAFFF>Chlorine Blue", "#0CAFFF".toColor()),
                    Colors.SubColor("<#00BFFF>Deep Sky Blue", "#0CBFFF".toColor()),
                    Colors.SubColor("<#7DF9FF>Eletric Blue", "#7DF9FF".toColor()),
                    Colors.SubColor("<#87CEFA>Light Sky Blue", "#87CEFA".toColor()),
                    Colors.SubColor("<#73C2FB>Maya Blue", "#73C2FB".toColor()),
                    Colors.SubColor("<#7CB9E8>Aero Blue", "#7CB9E8".toColor()),
                    Colors.SubColor("<#99FFFF>Ice Blue", "#99FFFF".toColor())
                )
            ),
            "yellow" to Colors(
                Colors.BaseColor("<#FFC900>Yellow", "#FFC900".toColor()),
                setOf(
                    Colors.SubColor("<#FDDE6C>Golden Rod", "#FDDE6C".toColor()),
                    Colors.SubColor("<#FFF44F>Lemon Yellow", "#FFF44F".toColor()),
                    Colors.SubColor("<#FFEF00>Canary Yellow", "#FFEF00".toColor()),
                    Colors.SubColor("<#FFD00A>Citrine Yellow", "#FFD00A".toColor()),
                    Colors.SubColor("<#F1FF62>Lazer Lemon Yellow", "#F1FF62".toColor()),
                    Colors.SubColor("<#E8DE2A>Sunflower Yellow", "#E8DE2A".toColor()),
                    Colors.SubColor("<#FFD800>School Bus Yellow", "#FFD800".toColor())
                )
            ),
            "pink" to Colors(
                Colors.BaseColor("<#FC35FA>Pink", "#FC35FA".toColor()),
                setOf(
                    Colors.SubColor("<#FC8EAC>Flamingo Pink", "#FC8EAC".toColor()),
                    Colors.SubColor("<#FC6C85>Watermelon Color", "#FC6C85".toColor()),
                    Colors.SubColor("<#FF77FF>Fuchsia Pink", "#FF77FF".toColor()),
                    Colors.SubColor("<#FF69B4>Hot Pink", "#FF69B4".toColor()),
                    Colors.SubColor("<#E75480>Dark Pink", "#E75480".toColor()),
                    Colors.SubColor("<#FFD1dDC>Pastel Pink", "#FFD1DC".toColor()),
                    Colors.SubColor("<#DE5D83>Blush Pink", "#8c0c1c".toColor())
                )
            ),
            "white" to Colors(
                Colors.BaseColor("<#FFFFFF>White", "#FFFFFF".toColor()),
                setOf(
                    Colors.SubColor("<#FFFFFF>White", "#FFFFFF".toColor()),
                    Colors.SubColor("<#FDF5E6>Old Lace White", "#FDF5E6".toColor()),
                    Colors.SubColor("<#FAF0E6>Linen White", "#FAF0E6".toColor()),
                    Colors.SubColor("<#F5F5F5>White Smoke", "#F5F5F5".toColor()),
                    Colors.SubColor("<#808080>Light Gray", "#808080".toColor()),
                    Colors.SubColor("<#2A2727>Dark Gray", "#2A2727".toColor()),
                    Colors.SubColor("<#000000>Black", "#000000".toColor())
                )
            ),
            "green" to Colors(
                Colors.BaseColor("<#A3DA8D>Green", "#A3DA8D".toColor()),
                setOf(
                    Colors.SubColor("<#32CD32>Lime Green", "#32CD32".toColor()),
                    Colors.SubColor("<#4CBB17>Kelly Green", "#4CBB17".toColor()),
                    Colors.SubColor("<#7FFF00>Chartreuse Green", "#7FFF00".toColor()),
                    Colors.SubColor("<#006400>Dark Green", "#006400".toColor()),
                    Colors.SubColor("<#228B22>Forest Green", "#228B22".toColor()),
                    Colors.SubColor("<#39FF14>Neon Green", "#39FF14".toColor()),
                    Colors.SubColor("<#4F7942>Fern Green", "#4F7942".toColor())
                )
            ),
            "purple" to Colors(
                Colors.BaseColor("<#8946A6>Purple", "#8946A6".toColor()),
                setOf(
                    Colors.SubColor("<#9966CC>Amethyst Purple", "#9966CC".toColor()),
                    Colors.SubColor("<#4B0082>Indigo Purple", "#4B0082".toColor()),
                    Colors.SubColor("<#551A8b>Dark Purple", "#551A8B".toColor()),
                    Colors.SubColor("<#6f2DA8>Grape Purple", "#6F2DA8".toColor()),
                    Colors.SubColor("<#BF00FF>Electric Purple", "#BF00FF".toColor()),
                    Colors.SubColor("<#7851A9>Royal Purple", "#7851A9".toColor()),
                    Colors.SubColor("<#6F2DA8>Lilac Purple", "#B666D2".toColor())
                )
            )
        )
    }
}


