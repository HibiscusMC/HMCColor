package com.hibiscusmc.hmccolor

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class HMCColorCommands : BasicCommand {

    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        if (args.isNotEmpty()) when (args[0]) {
            "reload" -> {
                hmcColor.plugin.createColorContext()
                source.sender.sendRichMessage("<green>HMCColor configs have been reloaded!")
            }
            "dye" -> HMCColorApi.openColorMenu(source.executor as Player)
        }
    }

    override fun suggest(source: CommandSourceStack, args: Array<out String>): Collection<String> {
        if (args.isEmpty()) return emptyList()
        return listOf("reload", "dye").filter { it.startsWith(args[0], true) }
    }
}