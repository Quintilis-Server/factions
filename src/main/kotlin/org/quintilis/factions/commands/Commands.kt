package org.quintilis.factions.commands

interface Commands {
    val command: String
    val usage: String
    val helpEntry: HelpEntry
    val subCommands: Array<out Commands>?
}