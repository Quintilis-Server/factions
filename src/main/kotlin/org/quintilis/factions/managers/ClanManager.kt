package org.quintilis.factions.managers

import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.exceptions.clan.ClanNotFoundError

object ClanManager {
    private val clanDao = DatabaseManager.getDAO(ClanDao::class)

    fun getClan(name: String): ClanEntity{
        return clanDao.findByName(name) ?: throw ClanNotFoundError()
    }

}