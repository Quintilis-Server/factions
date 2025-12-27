package org.quintilis.factions.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.quintilis.factions.exceptions.BaseError

object ErrorManager {
    inline fun runSafe(sender: CommandSender, block: () -> Unit) {
        try {
            block()
        } catch (e: BaseError) {
            // O Adventure resolve a tradução automaticamente baseado na língua do cliente (se configurado)
            // ou usa o arquivo de idioma padrão do servidor.
            sender.sendMessage(e.component.colorIfAbsent(NamedTextColor.RED))
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendMessage(
                Component.text("Erro interno. Contate a administração.", NamedTextColor.DARK_RED)
            )
        }
    }
}