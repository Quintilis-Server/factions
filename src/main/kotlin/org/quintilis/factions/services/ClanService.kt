package org.quintilis.factions.services

import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanMemberEntity
import org.quintilis.factions.results.ClanResult

/**
 * Serviço de lógica de negócio para operações de clã.
 * Centraliza validações e operações de CRUD.
 */
class ClanService {
    
    private val clanCache get() = Services.clanCache
    private val clanDao get() = Services.clanDao
    private val playerDao get() = Services.playerDao
    
    /**
     * Cria um novo clã.
     * 
     * Validações:
     * - Jogador não pode já estar em um clã
     * - Nome do clã não pode já existir
     */
    fun createClan(leader: Player, name: String, tag: String?): ClanResult {
        // Verifica se já está em um clã
        if (clanCache.isMember(leader.uniqueId)) {
            return ClanResult.Error("clan.already_in_clan")
        }
        
        // Verifica se é dono de outro clã
        if (clanCache.getClanByLeaderId(leader.uniqueId) != null) {
            return ClanResult.Error("clan.already_in_clan")
        }
        
        // Verifica se nome já existe
        if (clanCache.existsByName(name)) {
            return ClanResult.Error(
                "clan.create.error.already_exists",
                mapOf("clan_name" to name)
            )
        }
        
        // Cria o clã
        val clan = ClanEntity(
            name = name,
            tag = tag,
            leaderUuid = leader.uniqueId
        ).save<ClanEntity>()
        
        // Adiciona o líder como membro
        ClanMemberEntity(
            clanId = clan.id!!,
            playerId = leader.uniqueId
        ).save<ClanMemberEntity>()
        
        // Invalida caches
        clanCache.invalidateGlobalCaches()
        clanCache.invalidateMember(leader.uniqueId)
        
        return ClanResult.Success(
            "clan.create.response",
            mapOf("clan_name" to clan.name)
        )
    }
    
    /**
     * Deleta um clã.
     * 
     * Validações:
     * - Jogador deve ser o líder do clã
     */
    fun deleteClan(leader: Player): ClanResult {
        // Verifica se é líder
        val clan = clanCache.getClanByLeaderId(leader.uniqueId)
            ?: return ClanResult.Error("clan.is_not_leader")
        
        // Busca membros antes de deletar (para notificar)
        val members = clanCache.getMembers(clan.id!!)
        
        // Deleta o clã
        try {
            clanDao.deleteByIdAndLeader(clan.id)
        } catch (e: Exception) {
            e.printStackTrace()
            return ClanResult.Error("error.generic")
        }
        
        // Invalida caches
        clanCache.invalidateClan(clan)
        members.forEach { clanCache.invalidateMember(it.playerId) }
        
        return ClanResult.Success("clan.delete.response")
    }
    
    /**
     * Remove um membro de seu clã.
     * 
     * Validações:
     * - Jogador deve estar em um clã
     * - Jogador não pode ser o líder (deve usar deleteClan)
     */
    fun quitClan(member: Player): ClanResult {
        val uuid = member.uniqueId
        
        // Busca o clã do membro
        val clan = clanCache.getClanByMember(uuid)
            ?: return ClanResult.Error("error.not_in_clan")
        
        // Verifica se não é o líder
        if (clan.leaderUuid == uuid) {
            return ClanResult.Error("clan.quit.error.leader")
        }
        
        // Remove o membro
        clanDao.deleteMemberById(uuid)
        
        // Invalida caches
        clanCache.invalidateMember(uuid)
        clanCache.invalidateMembersOfClan(clan.id!!)
        
        return ClanResult.Success(
            "clan.quit.response",
            mapOf("clan_name" to clan.name, "leader_uuid" to clan.leaderUuid)
        )
    }
    
    /**
     * Lista clãs com paginação.
     */
    fun listClans(page: Int, pageSize: Int = 45): List<ClanEntity> {
        return clanCache.getClans(page, pageSize)
    }
    
    /**
     * Retorna o total de clãs.
     */
    fun getTotalClans(): Int {
        return clanCache.getTotalClans()
    }
}
