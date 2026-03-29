package org.quintilis.factions.services

import org.quintilis.factions.Factions
import org.quintilis.factions.cache.AllyInviteCache
import org.quintilis.factions.cache.ChunkCache
import org.quintilis.factions.cache.ClanCache
import org.quintilis.factions.cache.CoreCache
import org.quintilis.factions.cache.MemberInviteCache
import org.quintilis.factions.cache.PlayerCache
import org.quintilis.factions.dao.AllyInviteDao
import org.quintilis.factions.dao.ChunkDao
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.ClanRelationDao
import org.quintilis.factions.dao.CoreDao
import org.quintilis.factions.dao.MemberInviteDao
import org.quintilis.factions.dao.PlayerDao
import org.quintilis.factions.managers.DatabaseManager

/**
 * Singleton de serviços - ponto central de acesso a DAOs e Caches.
 * Evita criar múltiplas instâncias e facilita injeção de dependência.
 */
object FactionsServices {

    private lateinit var plugin: Factions

    fun init(plugin: Factions) {
        this.plugin = plugin
    }
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

    val chunkDao: ChunkDao by lazy {
        DatabaseManager.getDAO(ChunkDao::class)
    }

    val coreDao: CoreDao by lazy {
        DatabaseManager.getDAO(CoreDao::class)
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

    val chunkCache: ChunkCache by lazy {
        ChunkCache(chunkDao)
    }

    val coreCache: CoreCache by lazy {
        CoreCache(coreDao)
    }
    // ============================================
    // Services
    // ============================================
    val clanService: ClanService by lazy {
        ClanService()
    }

    val coreService: CoreService by lazy {
        checkInitialized()
        CoreService(plugin)
    }

    val chunkService: ChunkService by lazy {
        checkInitialized()
        ChunkService()
    }

    private fun checkInitialized() {
        if (!::plugin.isInitialized) {
            throw IllegalStateException("Services.init(plugin) não foi chamado no onEnable!")
        }
    }
}