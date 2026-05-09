package org.quintilis.factions.listeners

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.ChatLog
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.managers.ChatManager
import org.quintilis.factions.managers.TranslationManager

@AutoRegister
class ChatListener(private val plugin: JavaPlugin) : Listener {
    private val mm = MiniMessage.miniMessage()

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val clan = player.getClan()

        val originalMessage = PlainTextComponentSerializer.plainText().serialize(event.message())

        val isPrefix = originalMessage.startsWith("@")

        val isToggled = ChatManager.isClanChatToggled(player)

        if (isPrefix || isToggled) {
            if(clan == null){
                event.isCancelled = true
                player.sendTranslatable("error.no_clan")

                if(isToggled) ChatManager.toggleClanChat(player)
                return
            }

            event.isCancelled = true

            val finalMessage = if (isPrefix) originalMessage.substring(1).trim() else originalMessage.trim()

            if(finalMessage.isEmpty()) return

            val membersOnline = clan.getOnlinePlayers()

            for(member in membersOnline) {
                val chatFormat = TranslationManager.render(
                    "clan.chat.format",
                    member.locale(),
                    Placeholder.unparsed("player_name", player.name),
                    Placeholder.unparsed("message", finalMessage)
                )
                member.sendMessage(chatFormat)
            }

            Bukkit.getServer().asyncScheduler.runNow(plugin) { _ ->
                ChatLog(
                    playerId = player.uniqueId,
                    content = finalMessage,
                    channel = "CLAN", // Define o canal
                    clanId = clan.id,
                    locationX = player.location.blockX,
                    locationZ = player.location.blockZ,
                    worldUuid = player.world.uid
                ).save<BaseEntity>()
            }

            Bukkit.getConsoleSender().sendMessage(
                mm.deserialize("<dark_aqua>[Clã: ${clan.name}] <aqua>${player.name}</aqua>: <white>$finalMessage")
            )

        } else{
            Bukkit.getServer().asyncScheduler.runNow(plugin) { _ ->
                try {
                    ChatLog(
                        playerId = player.uniqueId,
                        content = originalMessage,
                        channel = "GLOBAL",
                        clanId = clan?.id, // Pode ser null se ele não tiver clã
                        locationX = player.location.blockX,
                        locationZ = player.location.blockZ,
                        worldUuid = player.world.uid
                    ).save<BaseEntity>()
                } catch (e: Exception) {
                    plugin.logger.warning("Falha ao salvar log do chat global de ${player.name}: ${e.message}")
                }
            }
        }

    }
}