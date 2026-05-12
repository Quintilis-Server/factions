package org.quintilis.factions.events

import org.bukkit.event.Event
import org.quintilis.factions.entities.clan.ClanEntity

/**
 * Evento base para qualquer ação que envolva um clã.
 * @param clan O clã envolvido no evento.
 * @param isAsync Se o evento está sendo disparado fora da thread principal (ex: durante um chat).
 */
abstract class BaseClanEvent(
    val clan: ClanEntity,
    isAsync: Boolean = false
) : Event(isAsync)