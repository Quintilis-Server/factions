package org.quintilis.factions.cache

import org.quintilis.factions.dao.AllyInviteDao
import org.quintilis.factions.managers.ConfigManager

/**
 * Cache para convites de aliança entre clãs.
 * Armazena os nomes dos clãs que enviaram convites para um clã alvo.
 */
class AllyInviteCache(
    private val allyInviteDao: AllyInviteDao,
): StringSetCache<Int>(
    prefix = "factions:invites:ally:",
    ttl = ConfigManager.getMaxAllyInvitationTime() * 86400L, // dias para segundos
){
    // Cache para clãs que ENVIARAM convite (usado no accept/reject)
    private val senderNamesCache = object : StringSetCache<Int>(
        prefix = "factions:invites:ally:senders:",
        ttl = ConfigManager.getMaxAllyInvitationTime() * 86400L,
    ) {}
    
    // Cache para clãs ALVO do convite (usado para listar convites enviados)
    private val targetNamesCache = object : StringSetCache<Int>(
        prefix = "factions:invites:ally:targets:",
        ttl = ConfigManager.getMaxAllyInvitationTime() * 86400L,
    ) {}
    
    /**
     * Retorna os nomes dos clãs que enviaram convite de aliança para o clã com o ID informado.
     * Usado no tab completer de /clan ally accept e /clan ally reject
     */
    fun getSenderClanNames(targetClanId: Int): List<String> {
        return senderNamesCache.getOrFetch(targetClanId) { id ->
            allyInviteDao.findSenderClanNamesForInvites(id)
        }
    }

    /**
     * Retorna os nomes dos clãs para os quais o clã com o ID informado enviou convite.
     */
    fun getTargetClanNames(senderClanId: Int): List<String> {
        return targetNamesCache.getOrFetch(senderClanId) { id ->
            allyInviteDao.findTargetClanNamesForInvites(id)
        }
    }
    
    /**
     * Invalida cache de convites para um clã específico.
     */
    override fun invalidate(key: Int) {
        super.invalidate(key)
        senderNamesCache.invalidate(key)
        targetNamesCache.invalidate(key)
    }
}
