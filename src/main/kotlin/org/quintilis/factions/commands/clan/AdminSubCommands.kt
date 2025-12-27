package org.quintilis.factions.commands.clan

import org.quintilis.factions.commands.Commands
import org.quintilis.factions.commands.HelpEntry

enum class AdminSubCommands(
    override val command: String,
    override val usage: String,
    override val helpEntry: HelpEntry,
    override val subCommands: Array<out Commands>? = null
): Commands {
    DELETE(
        "delete",
        "/clan admin delete <clan>",
        HelpEntry(
            "clan.admin.delete.description",
            "factions.admin"
        )
    ),
    SETNAME(
        "setname",
        "/clan admin setname <clan> <novo_nome>",
        HelpEntry(
            "clan.admin.setname.description",
            "factions.admin"
        )
    ),
    SETTAG(
        "settag",
        "/clan admin settag <clan> <nova_tag>",
        HelpEntry(
            "clan.admin.settag.description",
            "factions.admin"
        )
    ),
    SETLEADER(
        "setleader",
        "/clan admin setleader <clan> <jogador>",
        HelpEntry(
            "clan.admin.setleader.description",
            "factions.admin"
        )
    )
}
