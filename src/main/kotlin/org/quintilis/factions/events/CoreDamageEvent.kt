package org.quintilis.factions.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.quintilis.factions.entities.clan.AntiCoreEntity
import org.quintilis.factions.entities.clan.ClanCoreEntity

/**
 * Evento disparado quando um dano é feito em cima de um nexus ou core
 * é associado ao clã de quem atacou, clã de quem recebeu, dano, e coordenada
 */
class CoreDamageEvent(
    val antiCore: AntiCoreEntity,
    val targetCore: ClanCoreEntity,
    var damage: Int,
    private var isCancelled: Boolean = false
): Event(), Cancellable {


    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.isCancelled = cancel
    }
}