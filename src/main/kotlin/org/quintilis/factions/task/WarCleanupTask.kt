package org.quintilis.factions.task

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.quintilis.factions.Factions
import org.quintilis.factions.annotations.AutoTask
import org.quintilis.factions.entities.clan.ClanRelationEntity
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.QuintilisScheduler
import org.quintilis.factions.managers.RedisManager
import org.quintilis.factions.services.FactionsServices.antiCoreCache
import org.quintilis.factions.services.FactionsServices.clanCache
import org.quintilis.factions.services.FactionsServices.clanRelationCache

//@AutoTask(period = 1200L)
class WarCleanupTask(private val plugin: Factions): BukkitRunnable() {
    override fun run() {
        val relations = clanRelationCache.findAllEnemyActive()
        val now = System.currentTimeMillis()
        val timeoutMs = ConfigManager.getWarTimeout() * 1000L // Ex: 15 min em ms

        for (relation in relations) {
            val clan1Id = relation.clan1Id
            val clan2Id = relation.clan2Id

            // Checamos o pulso dos dois lados da guerra
            val heartbeat1 = getHeartbeat(clan1Id, clan2Id)
            val heartbeat2 = getHeartbeat(clan2Id, clan1Id)

            // A guerra só continua se PELO MENOS UM dos lados teve atividade recente
            val lastActivity = maxOf(heartbeat1, heartbeat2)

            if (now - lastActivity > timeoutMs) {
                // GUERRA ESFRIOU: Finalizar de forma assíncrona para não travar a Main Thread
                QuintilisScheduler.runGlobal(plugin, Runnable {
                    endWarByInactivity(relation)
                })
            }
        }
    }

    private fun getHeartbeat(attackerId: Int, defenderId: Int): Long {
        return RedisManager.run { jedis ->
            jedis.get("factions:war:heartbeat:$attackerId:$defenderId")?.toLongOrNull() ?: 0L
        }
    }

    private fun endWarByInactivity(relation: ClanRelationEntity) {
        val clan1 = clanCache.findById(relation.clan1Id) ?: return
        val clan2 = clanCache.findById(relation.clan2Id) ?: return

        // 1. Desativa a relação no banco e cache
        relation.deactivate()

        // 2. Limpa TODOS os Anti-Cores ativos entre esses dois clãs
        // Dica: Crie esse método no seu AntiCoreCache para facilitar
        antiCoreCache.deactivateAllBetween(clan1.id!!, clan2.id!!)

        // 3. Broadcast épico de encerramento
        Bukkit.broadcast(
            Component.translatable(
            "war.ended.inactivity",
            Argument.string("attacker", clan1.name),
            Argument.string("defender", clan2.name)
        ))

        plugin.logger.info("Guerra entre ${clan1.name} e ${clan2.name} encerrada por inatividade.")
    }
}