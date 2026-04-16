package org.quintilis.factions.cache

import org.quintilis.factions.dao.ClanMemberDao
import org.quintilis.factions.entities.clan.ClanMemberEntity

class ClanMemberCache(
    daoImpl: ClanMemberDao

): AbstractDaoCache<ClanMemberDao, ClanMemberEntity, Int>(
    dao = daoImpl,
    prefix = "clan_member:",
    ttl = 1200,
    classType = ClanMemberEntity::class.java,
), ClanMemberDao by daoImpl