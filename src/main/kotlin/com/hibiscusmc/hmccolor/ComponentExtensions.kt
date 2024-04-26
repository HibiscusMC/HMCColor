package com.hibiscusmc.hmccolor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.StyleBuilderApplicable
import net.kyori.adventure.text.format.TextDecoration

fun Component.noItalic() = this.style(this.style().edit { it.decoration(TextDecoration.ITALIC, false) })