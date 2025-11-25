package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanMemberEntity
import java.lang.IllegalArgumentException
import java.util.UUID

interface ClanDao: BaseDao {

    @SqlQuery("SELECT * FROM clans WHERE active = true ORDER BY id LIMIT :pageSize OFFSET :startPage")
    fun findWithPage(@Bind("startPage")startPage: Int, @Bind("pageSize")pageSize: Int): List<ClanEntity>

    @SqlQuery("SELECT COUNT(*) FROM clans WHERE active = TRUE")
    fun totalClans(): Int

    @SqlQuery("SELECT * FROM clans WHERE name LIKE '%' || :name || '%'")
    fun findByName(@Bind("name") name: String): ClanEntity?

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clans WHERE name = :name)")
    fun existsByName(@Bind("name") name: String): Boolean

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clans WHERE tag = :tag)")
    fun existsByTag(@Bind("tag") tag: String): Boolean

    @SqlQuery("SELECT * FROM clans WHERE leader_uuid = :leaderId")
    fun findByLeaderId(@Bind("leaderId") leaderId: UUID): ClanEntity?

    @Transaction
    fun deleteByIdAndLeader(id: Int, leaderId: UUID){
        val count = softDeleteClan(id, leaderId)

        if(count == 0){
            throw IllegalArgumentException("Clã não encontrado ou você não éo lider.")
        }

        deactivateClanMembers(clanId = id)
    }

    @SqlUpdate("UPDATE clans SET active = false WHERE id = :id AND leader_uuid = :leaderId")
    fun softDeleteClan(@Bind("id")id: Int,@Bind("leaderId") leaderId: UUID): Int

    @SqlUpdate("UPDATE clan_member SET active = false WHERE clan_id = :clanId AND active = true")
    fun deactivateClanMembers(@Bind("clanId") clanId: Int)

    @SqlQuery("SELECT * FROM clan_member WHERE clan_id = :clanId AND active = true")
    fun findMembersByClan(@Bind("clanId") clanId: Int): List<ClanMemberEntity>
}