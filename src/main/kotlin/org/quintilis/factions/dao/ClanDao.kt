package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.clan.ClanEntity

interface ClanDao: BaseDao {
    @SqlQuery("SELECT * FROM clans WHERE name LIKE '%' || :name || '%'")
    fun findByName(@Bind("name") name: String): ClanEntity?

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clans WHERE name = :name)")
    fun existsByName(@Bind("name") name: String): Boolean

    @SqlQuery("SELECT EXISTS(SELECT 1 FROM clans WHERE tag = :tag)")
    fun existsByTag(@Bind("tag") tag: String): Boolean
}