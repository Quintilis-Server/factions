package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.factions.entities.invite.InviteStatus
import org.quintilis.factions.entities.invite.member.MemberInviteEntity
import java.util.UUID

interface MemberInviteDao: BaseDao<MemberInviteEntity, Int>{
    @SqlQuery("SELECT * FROM member_invite WHERE clan_id = :clanId AND status = 'PENDING' AND active = true")
    fun findByClanId(@Bind("clanId") clanId: Int): List<MemberInviteEntity>

    @SqlQuery("SELECT * FROM member_invite WHERE player_id = :playerId AND status = 'PENDING' AND active = true")
    fun findByPlayer(@Bind("playerId") playerId: UUID): List<MemberInviteEntity>

    @SqlQuery("""
        SELECT *
            FROM member_invite 
        WHERE 
            player_id = :playerId 
            AND clan_id = :clanId
            AND status = 'PENDING'
            AND active = true
        """)
    fun findByPlayerIdAndClanId(@Bind("playerId") playerId: UUID, @Bind("clanId") clanId: Int): MemberInviteEntity?

    /**
     * Marca como `EXPIRED` e desativa todos os convites que
     * 1. Continuam `PENDING`.
     * 2. Estão `active = true`.
     * 3. O tempo de expiração já passou `expires_at < now()`.
     * @return' Quantidade de convites expirados.
     */
    @Transaction
    @SqlUpdate("""
        UPDATE member_invite
        SET status = 'EXPIRED', active = false
        WHERE status = 'PENDING'
            AND active = true
            AND expires_at < now()
    """)
    fun expireOverdueInvites(): Int

    @SqlQuery("""
        SELECT c.name 
        FROM member_invite i
        JOIN clans c ON i.clan_id = c.id
        WHERE i.player_id = :playerId 
          AND i.active = true 
          AND i.status = 'PENDING'
    """)
    fun findClanNamesForInvites(@Bind("playerId") playerId: UUID): List<String>

    @SqlQuery("""
        SELECT p.name
        FROM member_invite i
        JOIN players p ON i.player_id = p.id
        JOIN clans c ON i.clan_id = c.id
        WHERE c.leader_uuid = :leaderId
        AND i.active = true
        AND i.status = 'PENDING'
    """)
    fun findPlayerNamesForInvites(@Bind("leaderId") leaderId: UUID): List<String>

    @SqlUpdate("""
        UPDATE member_invite
        SET active = false, status = 'CANCELLED'
        WHERE player_id = :playerId
        AND active = true
        AND status = 'PENDING'
        RETURNING *;
    """)
    fun cancelInvite(@Bind("playerId") playerId: UUID)

    @SqlQuery("""SELECT EXISTS(
            SELECT 1 FROM member_invite 
            WHERE ACTIVE = 1 AND 
            clan_id = :clanId AND 
            player_id = :playerId AND
            status = 'PENDING'
        )
    """)
    fun hasInvite(@Bind("playerId") playerId: UUID, @Bind("clanId") clanId: Int): Boolean
}