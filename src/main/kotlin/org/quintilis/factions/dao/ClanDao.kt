package org.quintilis.factions.dao

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanMemberEntity
import java.lang.IllegalArgumentException
import java.util.UUID

interface ClanDao: BaseDao<ClanEntity, Int> {

    override fun getEntityClass() = ClanEntity::class.java

    @SqlQuery("SELECT * FROM clans WHERE active = true ORDER BY id LIMIT :pageSize OFFSET :startPage")
    fun findWithPage(@Bind("startPage")startPage: Int, @Bind("pageSize")pageSize: Int): List<ClanEntity>

    @SqlQuery("SELECT COUNT(*) FROM clans WHERE active = TRUE")
    fun totalClans(): Int

    @SqlQuery("SELECT * FROM clans WHERE name LIKE '%' || :name || '%' AND active = true")
    fun findByName(@Bind("name") name: String): ClanEntity?

    @SqlQuery("SELECT tag FROM clans WHERE id != :id AND active = true")
    fun listTags(@Bind("id") id: Int): List<String>

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clans WHERE name = :name AND active = true)")
    fun existsByName(@Bind("name") name: String): Boolean

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clans WHERE tag = :tag AND active = true)")
    fun existsByTag(@Bind("tag") tag: String): Boolean

    @SqlQuery("SELECT * FROM clans WHERE leader_uuid = :leaderId AND active = TRUE")
    fun findByLeaderId(@Bind("leaderId") leaderId: UUID): ClanEntity?

    @SqlQuery("""
        SELECT c.* 
        FROM clans c
            JOIN clan_member cm ON c.id = cm.clan_id
        WHERE cm.player_id = :playerId
            AND cm.active = true
            AND c.active = true;
    """)
    fun findByMember(@Bind("playerId") playerId: UUID): ClanEntity?

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clan_member WHERE player_id = :playerId AND active = true)")
    fun isMember(@Bind("playerId") playerId: UUID): Boolean

    @Transaction
    fun deleteByIdAndLeader(id: Int){
        val count = softDeleteClan(id)

        if(count == 0){
            throw IllegalArgumentException("Clã não encontrado ou você não éo lider.")
        }

        deactivateClanMembers(clanId = id)
    }

    @Transaction
    fun deleteMemberById(memberId: UUID){
        deactivateMember(playerId =  memberId)
    }

    @SqlUpdate("UPDATE clans SET active = false, deleted_at = now() WHERE id = :id")
    fun softDeleteClan(@Bind("id")id: Int): Int

    @SqlUpdate("UPDATE clan_member SET active = false WHERE clan_id = :clanId AND active = true")
    fun deactivateClanMembers(@Bind("clanId") clanId: Int)

    @SqlUpdate("UPDATE clan_member SET active = false, updated_at = now() WHERE active = true AND player_id = :playerId")
    fun deactivateMember(@Bind("playerId") playerId: UUID)

    @SqlQuery("SELECT * FROM clan_member WHERE clan_id = :clanId AND active = true")
    fun findMembersByClan(@Bind("clanId") clanId: Int): List<ClanMemberEntity>

    @SqlQuery("""
        SELECT c.name
        FROM clans c
        WHERE c.active = true
        """)
    fun findNames(): List<String>

    @SqlUpdate("UPDATE clans SET leader_uuid = :newLeaderId WHERE id = :clanId AND active = true")
    fun updateLeader(@Bind("clanId") clanId: Int, @Bind("newLeaderId") newLeaderId: UUID)

    @SqlUpdate("UPDATE clans SET name = :newName WHERE id = :clanId AND active = true")
    fun updateName(@Bind("clanId") clanId: Int, @Bind("newName") newName: String)

    @SqlUpdate("UPDATE clans SET tag = :newTag WHERE id = :clanId AND active = true")
    fun updateTag(@Bind("clanId") clanId: Int, @Bind("newTag") newTag: String?)
}