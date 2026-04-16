package org.quintilis.factions.services

import fr.skytasul.glowingentities.GlowingBlocks
import fr.skytasul.glowingentities.GlowingEntities
import org.quintilis.factions.Factions
import org.quintilis.factions.cache.AllyInviteCache
import org.quintilis.factions.cache.AntiCoreCache
import org.quintilis.factions.cache.ChunkCache
import org.quintilis.factions.cache.ClanCache
import org.quintilis.factions.cache.ClanChunkCache
import org.quintilis.factions.cache.ClanMemberCache
import org.quintilis.factions.cache.ClanRelationCache
import org.quintilis.factions.cache.CoreCache
import org.quintilis.factions.cache.MemberInviteCache
import org.quintilis.factions.cache.PlayerCache
import org.quintilis.factions.dao.AllyInviteDao
import org.quintilis.factions.dao.AntiCoreDao
import org.quintilis.factions.dao.ChunkDao
import org.quintilis.factions.dao.ClanChunkDao
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.dao.ClanMemberDao
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
    lateinit var glowingBlocks: GlowingBlocks
    lateinit var glowingEntities: GlowingEntities

    fun init(plugin: Factions) {
        this.plugin = plugin
        this.glowingBlocks = GlowingBlocks(plugin)
        this.glowingEntities = GlowingEntities(plugin)
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

    val clanMemberDao: ClanMemberDao by lazy {
        DatabaseManager.getDAO(ClanMemberDao::class)
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

    val clanChunkDao: ClanChunkDao by lazy {
        DatabaseManager.getDAO(ClanChunkDao::class)
    }

    val coreDao: CoreDao by lazy {
        DatabaseManager.getDAO(CoreDao::class)
    }

    val antiCoreDao: AntiCoreDao by lazy {
        DatabaseManager.getDAO(AntiCoreDao::class)
    }

    // ============================================
    // Caches
    // ============================================
    val clanCache: ClanCache by lazy { 
        ClanCache(clanDao) 
    }

    val clanRelationCache: ClanRelationCache by lazy {
        ClanRelationCache(clanRelationDao)
    }

    val clanMemberCache: ClanMemberCache by lazy {
        ClanMemberCache(clanMemberDao)
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

    val clanChunkCache: ClanChunkCache by lazy {
        ClanChunkCache(clanChunkDao)
    }

    val coreCache: CoreCache by lazy {
        CoreCache(coreDao)
    }

    val antiCoreCache: AntiCoreCache by lazy {
        AntiCoreCache(antiCoreDao)
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