package org.quintilis.factions.entities.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.quintilis.factions.entities.models.Clan
import org.quintilis.factions.entities.models.PlayerEntity
import org.quintilis.factions.managers.DatabaseManager
import java.sql.Timestamp

object InviteManager {

    var playerExpirationHours: Int = 1;

    fun setConfig( playerExpirationHours: Int?) {
        this.playerExpirationHours = playerExpirationHours?:1
    }

    fun addPlayerInvite(sender: PlayerEntity, receiver: PlayerEntity, clan: Clan) {
        val expireDate = Timestamp(System.currentTimeMillis() + (playerExpirationHours * 60 * 60 * 1000))

        DatabaseManager.jdbi.useHandle<Exception> { handle ->
            handle.createUpdate(
                """
            INSERT INTO clan_invites (sender_id, receiver_id, clan_id, expire_date)
            VALUES (:senderId, :receiverId, :clanId, :expireDate)
            """
            )
                .bind("senderId", sender.id)
                .bind("receiverId", receiver.id)
                .bind("clanId", clan.id)
                .bind("expireDate", expireDate)
                .execute()
        }

        val receiverPlayer = Bukkit.getPlayer(receiver.id!!)
        if (receiverPlayer != null) {
            val clanNameComponent = Component.text(clan.name, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
            val commandComponent = Component.text("/invite accept ${clan.name}", NamedTextColor.GREEN).decorate(TextDecoration.BOLD)

            receiverPlayer.sendMessage(
                Component.text("O clã ")
                    .append(clanNameComponent)
                    .append(Component.text(" quer te convidar para se unir. Use: "))
                    .append(commandComponent)
                    .append(Component.text(" para aceitar."))
            )
        }
    }
}