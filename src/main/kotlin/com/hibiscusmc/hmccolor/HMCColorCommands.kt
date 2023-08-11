package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.commands.arguments.playerArg
import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.messaging.success
import org.bukkit.Bukkit
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
                    HMCColorApi.colorMenu().open(player)
                }
            }
            "reload" {
                action {
                    hmcColor.plugin.createColorContext()
                    cachedDyeMap = getDyeColorList()
                    cachedEffectSet = getEffectList()
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
            1 -> listOf("dye", "reload")
            2 -> when (args[0]) {
                "dye" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1]) }
                else -> listOf()
            }
            else -> listOf()
        } else listOf()
    }
}
