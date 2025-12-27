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
    /**
     * Retorna os nomes dos clãs que enviaram convite de aliança para o clã com o ID informado.
     */
    fun getSenderClanNames(targetClanId: Int): List<String> {
        return getOrFetch(targetClanId) { id ->
            allyInviteDao.findSenderClanNamesForInvites(id)
        }
    }

    /**
     * Retorna os nomes dos clãs para os quais o clã com o ID informado enviou convite.
     */
    fun getTargetClanNames(senderClanId: Int): List<String> {
        return getOrFetch(senderClanId) { id ->
            allyInviteDao.findTargetClanNamesForInvites(id)
        }
    }
}
