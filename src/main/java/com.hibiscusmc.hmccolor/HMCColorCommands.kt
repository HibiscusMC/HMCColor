package com.hibiscusmc.hmccolor

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class HMCColorCommands : BasicCommand {

    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        if (args.isNotEmpty()) when (args[0]) {
            "reload" -> {
                hmcColor.plugin.createColorContext()
                source.sender.sendRichMessage("<green>HMCColor configs have been reloaded!")
            }
            "dye" ->  {
                val player = args.getOrNull(1)?.let(Bukkit::getPlayer) ?: source.sender as? Player ?: return
                HMCColorApi.openColorMenu(player)
            }
        }
    }

    override fun suggest(source: CommandSourceStack, args: Array<out String>): Collection<String> {
        if (args.isEmpty()) return emptyList()
        return when (args.size) {
            1 -> listOf("reload", "dye").filter { it.startsWith(args[0], true) }
            2 if args[0] == "dye" -> Bukkit.getOnlinePlayers().mapNotNull { p -> p.name.takeIf { it.startsWith(args[0], true) } }
            else -> emptyList()
        }
    }
}