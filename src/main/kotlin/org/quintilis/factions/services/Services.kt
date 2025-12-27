package org.quintilis.factions.services

import org.quintilis.factions.cache.AllyInviteCache
import org.quintilis.factions.cache.ClanCache
import org.quintilis.factions.cache.MemberInviteCache
import org.quintilis.factions.cache.PlayerCache
import org.quintilis.factions.dao.AllyInviteDao
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.ClanRelationDao
import org.quintilis.factions.dao.MemberInviteDao
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.managers.DatabaseManager

/**
 * Singleton de serviços - ponto central de acesso a DAOs e Caches.
 * Evita criar múltiplas instâncias e facilita injeção de dependência.
 */
object Services {
    // ============================================
    // DAOs
    // ============================================
    val clanDao: ClanDao by lazy { 
        DatabaseManager.getDAO(ClanDao::class) 
    }
    
    val playerDao: PlayerDao by lazy { 
        DatabaseManager.getDAO(PlayerDao::class) 
    }
    
    val clanRelationDao: ClanRelationDao by lazy { 
        DatabaseManager.getDAO(ClanRelationDao::class) 
    }
    
    val memberInviteDao: MemberInviteDao by lazy { 
        DatabaseManager.getDAO(MemberInviteDao::class) 
    }
    
    val allyInviteDao: AllyInviteDao by lazy { 
        DatabaseManager.getDAO(AllyInviteDao::class) 
    }
    
    // ============================================
    // Caches
    // ============================================
    val clanCache: ClanCache by lazy { 
        ClanCache(clanDao) 
    }
    
    val playerCache: PlayerCache by lazy { 
        PlayerCache(playerDao) 
    }
    
    val memberInviteCache: MemberInviteCache by lazy { 
        MemberInviteCache(memberInviteDao) 
    }
    
    val allyInviteCache: AllyInviteCache by lazy { 
        AllyInviteCache(allyInviteDao) 
    }
}