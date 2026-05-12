package org.quintilis.factions.services

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.quintilis.factions.entities.clan.ClanCoreEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanMemberEntity
import org.quintilis.factions.entities.log.ActionLogEntity
import org.quintilis.factions.entities.log.ActionLogType
import org.quintilis.factions.enums.CoreType
import org.quintilis.factions.events.ClanCreateEvent
import org.quintilis.factions.events.ClanDisbandEvent
import org.quintilis.factions.extensions.sendTranslatable
import org.quintilis.factions.results.Result
import org.quintilis.factions.services.FactionsServices.clanMemberCache

/**
 * Serviço de lógica de negócio para operações de clã.
 * Centraliza validações e operações de CRUD.
 */
class ClanService {
    
    private val clanCache get() = FactionsServices.clanCache
    private val clanDao get() = FactionsServices.clanDao
    private val playerDao get() = FactionsServices.playerDao
    private val coreService get() = FactionsServices.coreService
    /**
     * Cria um novo clã.
     * 
     * Validações:
     * - Jogador não pode já estar em um clã
     * - Nome do clã não pode já existir
     */
    fun createClan(leader: Player, name: String, tag: String?): Result {
        // Verifica se já está em um clã
        if (clanMemberCache.isAnyMember(leader.uniqueId)) {
            return Result.Error("error.already_in_clan")
        }
        
        // Verifica se é dono de outro clã
        if (clanCache.getClanByLeaderId(leader.uniqueId) != null) {
            return Result.Error("error.already_in_clan")
        }
        
        // Verifica se nome já existe
        if (clanCache.existsByName(name)) {
            return Result.Error(
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

        val core = ClanCoreEntity(
            clanId = clan.id,
            type = CoreType.NEXUS
        ).save<ClanCoreEntity>()



        // Invalida caches
        clanCache.invalidateGlobalCaches()
        clanCache.invalidateMember(leader.uniqueId)

        val nexusItem = coreService.createExistingNexusItem(core, leader.locale())

        val leftovers = leader.inventory.addItem(nexusItem)
        if(leftovers.isNotEmpty()) {
            leftovers.values.forEach { leftover ->
                leader.world.dropItem(leader.location, leftover)
            }
            leader.sendTranslatable("clan.create.dropped_item")
        }

        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.CLAN_CREATE,
            actorId = leader.uniqueId,
            clanId = clan.id,
            details = "Created clan: ${clan.name}"
        )

        val event = ClanCreateEvent(leader, clan)

        Bukkit.getPluginManager().callEvent(event)

        return Result.Success(
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
    fun deleteClan(leader: Player): Result {
        // Verifica se é líder
        val clan = clanCache.getClanByLeaderId(leader.uniqueId)
            ?: return Result.Error("clan.is_not_leader")
        
        // Busca membros antes de deletar (para notificar)
        val members = clanCache.getMembers(clan.id!!)
        
        // Deleta o clã
        try {
            clanDao.deleteByIdAndLeader(clan.id)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.Error("error.generic")
        }
        
        // Invalida caches
        clanCache.invalidateClan(clan)
        members.forEach { clanCache.invalidateMember(it.playerId) }
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.CLAN_DELETE,
            actorId = leader.uniqueId,
            clanId = clan.id,
            details = "Deleted clan: ${clan.name}"
        )

        val event = ClanDisbandEvent(clan, leader)
        Bukkit.getPluginManager().callEvent(event)

        return Result.Success("clan.delete.response")
    }
    
    /**
     * Remove um membro de seu clã.
     * 
     * Validações:
     * - Jogador deve estar em um clã
     * - Jogador não pode ser o líder (deve usar deleteClan)
     */
    fun quitClan(member: Player): Result {
        val uuid = member.uniqueId
        
        // Busca o clã do membro
        val clan = clanCache.getClanByMember(uuid)
            ?: return Result.Error("error.not_in_clan")
        
        // Verifica se não é o líder
        if (clan.leaderUuid == uuid) {
            return Result.Error("clan.quit.error.leader")
        }
        
        // Remove o membro
        clanDao.deleteMemberById(uuid)
        
        // Invalida caches
        clanCache.invalidateMember(uuid)
        clanCache.invalidateMembersOfClan(clan.id!!)
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.MEMBER_LEAVE,
            actorId = uuid,
            clanId = clan.id,
            details = "Left clan: ${clan.name}"
        )
        
        return Result.Success(
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
