package com.hibiscusmc.hmccolor

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

object Adventure {
    val SERIALIZER = LegacyComponentSerializer.builder()
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()
    val MINI_MESSAGE = MiniMessage.builder().tags(
        StandardTags.defaults()
    ).build()

    fun String.toLegacy() = SERIALIZER.serialize(MINI_MESSAGE.deserialize(this))
}
