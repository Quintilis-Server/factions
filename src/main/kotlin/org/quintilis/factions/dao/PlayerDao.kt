package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.entities.PlayerEntity
import java.util.UUID

interface PlayerDao: BaseDao {
    @SqlQuery("SELECT EXISTS (SELECT 1 FROM players WHERE id = :id);")
    fun isInDatabase(@Bind("id")id: UUID): Boolean

    @SqlQuery("SELECT * FROM players WHERE id = :id")
    fun findById(@Bind("id")id: UUID): PlayerEntity?
}