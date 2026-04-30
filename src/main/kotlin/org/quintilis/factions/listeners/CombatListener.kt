package org.quintilis.factions.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.managers.CombatManager
import org.quintilis.factions.services.FactionsServices.clanRelationCache

@AutoRegister
class CombatListener: Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCombat(event: EntityDamageByEntityEvent){
        val victim = event.entity as? Player ?: return
        val attacker = event.damager as? Player ?: return

        val victimClan = victim.getClan()
        val attackerClan = attacker.getClan()

        if(victimClan != null && attackerClan != null){
            if(victimClan.id == attackerClan.id) return

            if(clanRelationCache.isRelation(victimClan, attackerClan, Relation.ALLY)) return
        }
        CombatManager.tag(victim)
        CombatManager.tag(attacker)
    }
}