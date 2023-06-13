package com.hibiscusmc.hmccolor

import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.commands.extensions.actions.playerAction
import com.mineinabyss.idofront.messaging.success
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HMCColorCommands : IdofrontCommandExecutor(), TabCompleter {

    override val commands = commands(hmcColor.plugin) {
        "hmccolor" {
            "dye" {
                playerAction {
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
        return if (command.name == "hmccolor") {
            when (args.size) {
                1 -> listOf("dye", "reload")
                else -> listOf()
            }
        } else listOf()
    }
}
