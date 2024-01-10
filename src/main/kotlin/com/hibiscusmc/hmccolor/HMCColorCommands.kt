package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.commands.arguments.playerArg
import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.messaging.success
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class HMCColorCommands : IdofrontCommandExecutor(), TabCompleter {

    override val commands = commands(hmcColor.plugin) {
        "hmccolor" {
            "dye" {
                val player: Player by playerArg { default = sender as? Player }
                action {
                    player.openGui()
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
            2 -> (if (args[0] == "dye") hmcColor.plugin.server.onlinePlayers.map { it.name } else listOf()).filter { it.startsWith(args[1]) }
            else -> listOf()
        } else listOf()
    }
}
