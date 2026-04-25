package org.quintilis.factions.cache

import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.dao.ClanMemberDao
import org.quintilis.factions.entities.clan.ClanMemberEntity

class ClanMemberCache(
    daoImpl: ClanMemberDao,
    private val plugin: JavaPlugin
): AbstractDaoCache<ClanMemberDao, ClanMemberEntity, Int>(
    dao = daoImpl,
    prefix = "clan_member:",
    ttl = 1200,
    classType = ClanMemberEntity::class.java,
    plugin = plugin
), ClanMemberDao by daoImpl