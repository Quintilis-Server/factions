package org.quintilis.factions.commands.clan

import org.quintilis.factions.commands.Commands
import org.quintilis.factions.commands.HelpEntry

enum class ClaimSubCommands(
    override val command: String,
    override val usage: String,
    override val helpEntry: HelpEntry,
    override val subCommands: Array<out Commands>? = null
): Commands {
    BUY(
        "buy",
        "/clan claim buy",
        HelpEntry("clan.claim.buy.description", "factions.usage")
    ),
    
    NEXUS(
        "nexus",
        "/clan claim nexus",
        HelpEntry("clan.claim.nexus.description", "factions.usage")
    );

}