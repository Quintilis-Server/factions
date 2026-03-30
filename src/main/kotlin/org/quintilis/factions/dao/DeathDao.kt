package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.factions.entities.Death
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.Relation
import org.quintilis.factions.entities.player.PlayerEntity
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.services.FactionsServices.clanRelationCache

interface DeathDao: BaseDao<Death, Int> {
    @Transaction
    fun registerDeathAndAwardPoints(killer: PlayerEntity, victim: PlayerEntity) {
        if (killer.id == victim.id) return
        Death(
            playerId = victim.id,
            killerId = killer.id,
        ).save<Death>()
        val killerClan = killer.getClan()
        val victimClan = victim.getClan()

        if (killerClan != null && victimClan != null && killerClan.id == victimClan.id) {
            return
        }

        val pointsToAdd = if (killerClan != null && victimClan != null) {
            val isAlly = clanRelationCache.isRelation(killerClan.id!!, victimClan.id!!, Relation.ALLY)
            val isEnemy = clanRelationCache.isRelation(killerClan.id, victimClan.id, Relation.ENEMY)

            when {
                isAlly -> ConfigManager.getWarAllyPoints()
                isEnemy -> ConfigManager.getWarEnemyPoints()
                else -> ConfigManager.getWarNeutralPoints()
            }
        } else {
            // Se o killer ou a vítima não têm clã, é uma kill neutra
            ConfigManager.getWarNeutralPoints()
        }

        // 5. Entrega os pontos e salva no banco usando a sua BaseEntity
        if (killerClan != null) {
            killerClan.points += pointsToAdd
            killerClan.save<ClanEntity>() // Salva o Clã
        } else {
            killer.points += pointsToAdd
            killer.save<PlayerEntity>() // Salva o Jogador Solo
        }
    }
}