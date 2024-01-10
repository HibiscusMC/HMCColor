package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.textcomponents.miniMsg
import dev.triumphteam.gui.components.GuiType
import dev.triumphteam.gui.guis.Gui
import org.bukkit.entity.Player

object HMCColorApi {

    fun colorMenu(player: Player) = player.getGui()

    fun openColorMenu(player: Player) {
        player.openGui()
    }
}
