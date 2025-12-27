package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.factions.entities.invite.ally.AllyInviteEntity

interface AllyInviteDao: BaseDao<AllyInviteEntity, Int> {
    override fun getEntityClass() = AllyInviteEntity::class.java

    @SqlQuery("""
        SELECT EXISTS(
            SELECT 1
            FROM 
                ally_invite 
            WHERE
                sender_clan_id = :senderId AND
                target_clan_id = :targetId AND
                status = 'PENDING' AND
                active = true
                
        )
    """)
    fun hasInvite(@Bind("senderId") senderId: Int, @Bind("targetId") targetId: Int): Boolean


    /**
     * Marca como `EXPIRED` e desativa todos os convites que
     * 1. Continuam `PENDING`.
     * 2. Estão `active = true`.
     * 3. O tempo de expiração já passou `expires_at < now()`.
     * @return' Quantidade de convites expirados.
     */
    @Transaction
    @SqlQuery("""
        UPDATE ally_invite
        SET status = 'EXPIRED', active = false
        WHERE status = 'PENDING'
            AND active = true
            AND expires_at < now()
    """)
    fun expireOverdueInvites(): Int

    /**
     * Busca os nomes dos clãs que enviaram convite de aliança para o clã alvo.
     */
    @SqlQuery("""
        SELECT c.name FROM ally_invite ai
        JOIN clan c ON c.id = ai.sender_clan_id
        WHERE ai.target_clan_id = :targetClanId
            AND ai.status = 'PENDING'
            AND ai.active = true
    """)
    fun findSenderClanNamesForInvites(@Bind("targetClanId") targetClanId: Int): List<String>

    /**
     * Busca os nomes dos clãs para os quais o clã remetente enviou convite.
     */
    @SqlQuery("""
        SELECT c.name FROM ally_invite ai
        JOIN clan c ON c.id = ai.target_clan_id
        WHERE ai.sender_clan_id = :senderClanId
            AND ai.status = 'PENDING'
            AND ai.active = true
    """)
    fun findTargetClanNamesForInvites(@Bind("senderClanId") senderClanId: Int): List<String>
}