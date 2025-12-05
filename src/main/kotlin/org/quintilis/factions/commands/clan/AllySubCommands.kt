package org.quintilis.factions.commands.clan

import org.quintilis.factions.commands.Commands
import org.quintilis.factions.commands.HelpEntry

enum class AllySubCommands(
    override val command: String,
    override val usage: String,
    override val helpEntry: HelpEntry,
    override val subCommands: Array<out Commands>? = null
): Commands {
    ADD(
        "add",
        "/clan ally add <tag>",
        HelpEntry(
            "clan.ally.add.description",
            "factions.usage"
        )
    ),
    REMOVE(
        "remove",
        "/clan ally remove <tag>",
        HelpEntry(
            "clan.ally.remove.description",
            "factions.usage"
        )
    ),
    LIST(
        "list",
        "/clan ally list",
        HelpEntry(
            "clan.ally.list.description",
            "factions.usage"
        )
    ),
    ACCEPT(
        "accept",
        "/clan ally accept <tag>",
        HelpEntry(
            "clan.ally.accept.description",
            "factions.usage"
        )
    ),
    REJECT(
        "reject",
        "/clan ally reject <tag>",
        HelpEntry(
            "clan.ally.reject.description",
            "factions.usage"
        )
    )
}