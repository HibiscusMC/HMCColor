package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.commands.extensions.actions.playerAction
import com.mineinabyss.idofront.items.asColorable
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.messaging.logVal
import com.mineinabyss.idofront.messaging.success
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import com.mineinabyss.idofront.util.ColorHelpers
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HMCColorCommands : IdofrontCommandExecutor(), TabCompleter {

    @OptIn(ExperimentalStdlibApi::class)
    override val commands = commands(hmcColor.plugin) {
        "hmccolor" {
            "dye" {
                playerAction {
                    player.createColorMenu().open(player)
                }
            }
            "reload" {
                action {
                    hmcColor.plugin.createColorContext()
                    sender.success("HMCColor configs have been reloaded!")
                }
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return if (command.name == "hmccolor") when (args.size) {
            1 -> listOf("dye", "reload").filter { it.startsWith(args[0]) }
            2 -> (if (args[0] == "dye") if (sender.hasPermission(command.permission ?: "")) hmcColor.plugin.server.onlinePlayers.map { it.name } else listOf(sender.name) else listOf()).filter { it.startsWith(args[1]) }
            else -> listOf()
        } else listOf()
    }
}
