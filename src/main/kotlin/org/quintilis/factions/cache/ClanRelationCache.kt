package org.quintilis.factions.cache

import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.dao.ClanRelationDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanRelationEntity
import org.quintilis.factions.entities.clan.Relation
import java.time.Instant

class ClanRelationCache(
    private val daoImpl: ClanRelationDao,
    private val plugin: JavaPlugin
): ClanRelationDao by daoImpl {

    private val relationsCache = object : StringSetCache<Int>(
        prefix = "factions:relation:clan:",
        ttl = 3600L, // 1 hora de TTL
        plugin = this.plugin
    ) {}

    private fun getActiveRelations(clanId: Int): List<String> {
        return relationsCache.getOrFetch(clanId) { id ->
            val entities = daoImpl.findAllActiveByClanId(id)

            // Converte a lista de entidades para a lista de Strings otimizada
            entities.map { entity ->
                val targetId = if (entity.clan1Id == id) entity.clan2Id else entity.clan1Id
                "$targetId:${entity.relation.name}"
            }
        }
    }

    fun isRelation(clan: ClanEntity, targetId: ClanEntity, relation: Relation): Boolean{
        return this.isRelation(clan.id!!, targetId.id!!, relation)
    }

    override fun isRelation(clanId: Int, targetId: Int, relation: Relation): Boolean {
        // Puxa do Cache (que é muito mais rápido que a query isRelation do banco)
        val relations = getActiveRelations(clanId)

        // Verifica se a string exata existe na lista ("ID:RELAÇÃO")
        return relations.contains("$targetId:${relation.name}")
    }

    fun createRelation(clan1Id: Int, clan2Id: Int, relation: Relation) {
        // 1. Executa no Banco de Dados
        ClanRelationEntity(
            clan1Id = clan1Id,
            clan2Id = clan2Id,
            relation = relation,
        ).save<ClanRelationEntity>()

        // 2. Invalida o cache dos DOIS clãs
        relationsCache.invalidate(clan1Id)
        relationsCache.invalidate(clan2Id)
    }

    override fun removeRelation(clan1Id: Int, clan2Id: Int) {
        // 1. Executa no Banco de Dados
        daoImpl.removeRelation(clan1Id, clan2Id)

        // 2. Invalida o cache dos DOIS clãs
        relationsCache.invalidate(clan1Id)
        relationsCache.invalidate(clan2Id)
    }
}