package org.quintilis.factions.cache

import com.google.gson.reflect.TypeToken
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.clan.ClanMemberEntity
import org.quintilis.factions.managers.RedisManager
import redis.clients.jedis.Jedis
import java.util.UUID

class ClanCache(
    private val clanDao: ClanDao
): JsonCache<Int, ClanEntity>(
    prefix = "factions:clan:id:",
    ttl = 300L,
    classType = ClanEntity::class.java,
) {
    private val gson = GsonProvider.gson

    private val clanListType = object : TypeToken<List<ClanEntity>>() {}.type
    private val memberListType = object : TypeToken<List<ClanMemberEntity>>() {}.type

    private val PAGE_TTL = 60L
    private val SHORT_TTL = 30L

    // ============================================
    // Cache por página (listagem paginada)
    // ============================================
    private val pageCache = object : BaseRedisCache<Int, List<ClanEntity>>(
        keyPrefix = "factions:clan:page:",
        ttlSeconds = PAGE_TTL
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): List<ClanEntity>? {
            val json = jedis.get(key) ?: return null
            if (json.trim() == "[]" || json.isBlank()) return null
            return try {
                this@ClanCache.gson.fromJson<List<ClanEntity>>(json, clanListType)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: List<ClanEntity>) {
            if (value.isNotEmpty()) {
                try {
                    val json = this@ClanCache.gson.toJson(value, clanListType)
                    jedis.set(key, json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun shouldCache(value: List<ClanEntity>): Boolean = value.isNotEmpty()
    }

    // ============================================
    // Cache por nome do clã
    // ============================================
    private val nameCache = object : BaseRedisCache<String, ClanEntity?>(
        keyPrefix = "factions:clan:name:",
        ttlSeconds = 300L
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): ClanEntity? {
            val json = jedis.get(key) ?: return null
            return try {
                this@ClanCache.gson.fromJson(json, ClanEntity::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: ClanEntity?) {
            if (value != null) {
                try {
                    val json = this@ClanCache.gson.toJson(value)
                    jedis.set(key, json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun shouldCache(value: ClanEntity?): Boolean = value != null
    }

    // ============================================
    // Cache por UUID do líder
    // ============================================
    private val leaderCache = object : BaseRedisCache<UUID, ClanEntity?>(
        keyPrefix = "factions:clan:leader:",
        ttlSeconds = 300L
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): ClanEntity? {
            val json = jedis.get(key) ?: return null
            return try {
                this@ClanCache.gson.fromJson(json, ClanEntity::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: ClanEntity?) {
            if (value != null) {
                try {
                    val json = this@ClanCache.gson.toJson(value)
                    jedis.set(key, json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun shouldCache(value: ClanEntity?): Boolean = value != null
    }

    // ============================================
    // Cache por UUID do membro
    // ============================================
    private val memberClanCache = object : BaseRedisCache<UUID, ClanEntity?>(
        keyPrefix = "factions:clan:member:",
        ttlSeconds = 300L
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): ClanEntity? {
            val json = jedis.get(key) ?: return null
            return try {
                this@ClanCache.gson.fromJson(json, ClanEntity::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: ClanEntity?) {
            if (value != null) {
                try {
                    val json = this@ClanCache.gson.toJson(value)
                    jedis.set(key, json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun shouldCache(value: ClanEntity?): Boolean = value != null
    }

    // ============================================
    // Cache de membros do clã
    // ============================================
    private val membersCache = object : BaseRedisCache<Int, List<ClanMemberEntity>>(
        keyPrefix = "factions:clan:members:",
        ttlSeconds = 120L
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): List<ClanMemberEntity>? {
            val json = jedis.get(key) ?: return null
            if (json.trim() == "[]" || json.isBlank()) return null
            return try {
                this@ClanCache.gson.fromJson<List<ClanMemberEntity>>(json, memberListType)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: List<ClanMemberEntity>) {
            try {
                val json = this@ClanCache.gson.toJson(value, memberListType)
                jedis.set(key, json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun shouldCache(value: List<ClanMemberEntity>): Boolean = true
    }

    // ============================================
    // Cache de nomes de clãs (para autocomplete)
    // ============================================
    private val namesCache = object : StringSetCache<String>(
        prefix = "factions:clan:names",
        ttl = 120L
    ) {}

    // ============================================
    // Cache do total de clãs
    // ============================================
    private val totalCache = object : BaseRedisCache<String, Int>(
        keyPrefix = "factions:clan:total:",
        ttlSeconds = SHORT_TTL
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): Int? {
            val value = jedis.get(key) ?: return null
            return value.toIntOrNull()
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: Int) {
            jedis.set(key, value.toString())
        }

        override fun shouldCache(value: Int): Boolean = true
    }

    // ============================================
    // Métodos públicos de leitura
    // ============================================

    /**
     * Busca um clã pelo ID.
     */
    fun getClan(id: Int): ClanEntity? {
        return getOrFetch(id) { dbId ->
            clanDao.findById(dbId)
        }
    }

    /**
     * Busca clãs com paginação.
     */
    fun getClans(page: Int, pageSize: Int = 45): List<ClanEntity> {
        val result = pageCache.getOrFetch(page) { pageNum ->
            try {
                val offset = (pageNum - 1) * pageSize
                clanDao.findWithPage(offset, pageSize)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
        return result
    }

    /**
     * Busca um clã pelo nome.
     */
    fun getClanByName(name: String): ClanEntity? {
        return nameCache.getOrFetch(name.lowercase()) { dbName ->
            clanDao.findByName(dbName)
        }
    }

    /**
     * Busca um clã pelo UUID do líder.
     */
    fun getClanByLeaderId(leaderUuid: UUID): ClanEntity? {
        return leaderCache.getOrFetch(leaderUuid) { uuid ->
            clanDao.findByLeaderId(uuid)
        }
    }

    /**
     * Busca o clã de um membro pelo UUID.
     */
    fun getClanByMember(memberUuid: UUID): ClanEntity? {
        return memberClanCache.getOrFetch(memberUuid) { uuid ->
            clanDao.findByMember(uuid)
        }
    }

    /**
     * Busca membros de um clã pelo ID do clã.
     */
    fun getMembers(clanId: Int): List<ClanMemberEntity> {
        return membersCache.getOrFetch(clanId) { id ->
            clanDao.findMembersByClan(id)
        }
    }

    /**
     * Retorna o total de clãs ativos.
     */
    fun getTotalClans(): Int {
        return totalCache.getOrFetch("count") { _ ->
            clanDao.totalClans()
        }
    }

    /**
     * Retorna lista de nomes de clãs (para autocomplete).
     */
    fun getClanNames(): List<String> {
        return namesCache.getOrFetch("all") { _ ->
            clanDao.findNames()
        }
    }

    /**
     * Verifica se existe um clã com o nome (usa cache).
     */
    fun existsByName(name: String): Boolean {
        return getClanByName(name) != null
    }

    /**
     * Verifica se o jogador é membro de algum clã (usa cache).
     */
    fun isMember(playerUuid: UUID): Boolean {
        return getClanByMember(playerUuid) != null
    }

    // ============================================
    // Métodos de invalidação de cache
    // ============================================

    /**
     * Invalida cache de um clã específico (id, nome, líder).
     */
    fun invalidateClan(clan: ClanEntity) {
        if (clan.id != null) {
            invalidate(clan.id)
            membersCache.invalidate(clan.id)
        }
        nameCache.invalidate(clan.name.lowercase())
        leaderCache.invalidate(clan.leaderUuid)
        invalidateGlobalCaches()
    }

    /**
     * Invalida cache por UUID do membro.
     */
    fun invalidateMember(memberUuid: UUID) {
        memberClanCache.invalidate(memberUuid)
    }

    /**
     * Invalida cache de membros de um clã específico.
     */
    fun invalidateMembersOfClan(clanId: Int) {
        membersCache.invalidate(clanId)
    }

    /**
     * Invalida caches globais (total, nomes, páginas).
     */
    fun invalidateGlobalCaches() {
        totalCache.invalidate("count")
        namesCache.invalidate("all")
        // Invalida todas as páginas de cache
        try {
            RedisManager.run { jedis ->
                val keys = jedis.keys("factions:clan:page:*")
                if (keys.isNotEmpty()) {
                    jedis.del(*keys.toTypedArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Atualiza (invalida) o cache de um clã após modificação.
     */
    fun update(clan: ClanEntity) {
        invalidateClan(clan)
    }
}