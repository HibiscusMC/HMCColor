package com.hibiscusmc.hmccolor.extensions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration

fun Component.noItalic() = this.style(this.style().edit { it.decoration(TextDecoration.ITALIC, false) })