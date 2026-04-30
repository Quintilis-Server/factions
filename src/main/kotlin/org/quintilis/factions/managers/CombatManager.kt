package org.quintilis.factions.managers

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CombatManager {
    private val combatTimers = ConcurrentHashMap<UUID, Long>()

    private const val COMBAT_DURATION = 15L

    fun tag(player: Player) {
        val expireTime = System.currentTimeMillis() + (COMBAT_DURATION * 1000)
        combatTimers[player.uniqueId] = expireTime
    }

    fun isInCombat(player: Player): Boolean {
        val expireTime = combatTimers[player.uniqueId] ?: return false

        if(System.currentTimeMillis() > expireTime) {
            combatTimers.remove(player.uniqueId)
            return false
        }

        return true
    }

    fun getRemainingTime(player: Player): Int {
        val expireTime = combatTimers[player.uniqueId] ?: return 0
        val remaining = (expireTime - System.currentTimeMillis()) / 1000
        return remaining.toInt().coerceAtLeast(0)
    }
}