package org.quintilis.factions.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import org.quintilis.factions.dao.ClanDao
import org.quintilis.factions.entities.clan.ClanEntity
import redis.clients.jedis.Jedis
import java.time.OffsetDateTime
import java.util.UUID
import java.lang.reflect.Type
import java.time.format.DateTimeFormatter

class ClanCache(
    private val clanDao: ClanDao
): JsonCache<Int, ClanEntity>(
    prefix = "factions:clan:id:",
    ttl = 300L,
    classType = ClanEntity::class.java,
) {
    private val gson: Gson = GsonBuilder()
        // IMPORTANTE: Excluir campos marcados com @Transient (evita serializar KProperty1)
        .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT)
        // Adaptador para OffsetDateTime (Essencial!)
        .registerTypeAdapter(OffsetDateTime::class.java, object : JsonSerializer<OffsetDateTime>,
            JsonDeserializer<OffsetDateTime> {
            override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            }
            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): OffsetDateTime {
                return OffsetDateTime.parse(json!!.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }
        })
        // Adaptador para UUID (Prevenção)
        .registerTypeAdapter(UUID::class.java, object : JsonSerializer<UUID>, JsonDeserializer<UUID> {
            override fun serialize(src: UUID?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src.toString())
            }
            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UUID {
                return UUID.fromString(json!!.asString)
            }
        })
        .create()

    private val clanListType = object : TypeToken<List<ClanEntity>>() {}.type

    private val PAGE_TTL = 60L

    private val pageCache = object : BaseRedisCache<Int, List<ClanEntity>>(
        keyPrefix = "factions:clan:page:",
        ttlSeconds = PAGE_TTL // TTL curto para listagens
    ) {
        override fun readFromRedis(jedis: Jedis, key: String): List<ClanEntity>? {
//            println("[ClanCache.pageCache] readFromRedis chamado para key: $key")
            val json = jedis.get(key)
            
            if (json == null) {
//                println("[ClanCache.pageCache] JSON é null - retornando null")
                return null
            }
            
//            println("[ClanCache.pageCache] JSON lido (${json.length} chars): ${json}...")

            // Se o JSON for uma lista vazia "[]", ignoramos o cache e forçamos ir ao DB.
            // Isso resolve o problema de ficar retornando vazio se o cache estiver "sujo".
            if (json.trim() == "[]" || json.isBlank()) {
//                println("[ClanCache.pageCache] JSON é lista vazia - retornando null para forçar DB")
                return null
            }

            return try {
                // IMPORTANTE: Usa o gson customizado da classe externa (com adaptadores)
                val result = this@ClanCache.gson.fromJson<List<ClanEntity>>(json, clanListType)
//                println("[ClanCache.pageCache] Deserialização bem-sucedida: ${result?.size ?: 0} clãs")
                result
            } catch (e: Exception) {
//                println("[ClanCache.pageCache] ❌ Erro ao deserializar lista de clãs do Redis: ${e.message}")
                e.printStackTrace()
                null
            }
        }

        override fun writeToRedis(jedis: Jedis, key: String, value: List<ClanEntity>) {
            if (value.isNotEmpty()) {
                try {
                    // IMPORTANTE: Usa o gson customizado da classe externa (com adaptadores)
                    val json = this@ClanCache.gson.toJson(value, clanListType)
                    jedis.set(key, json)
//                    println("Cache atualizado para página: salvos ${value.size} clãs")
                } catch (e: Exception) {
//                    println("Erro ao serializar lista de clãs para Redis: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        // Garante que nunca salvaremos uma lista vazia no Redis
        override fun shouldCache(value: List<ClanEntity>): Boolean {
            return value.isNotEmpty()
        }
    }
    /**
     * Busca um clã pelo ID.
     * 1. Tenta Redis.
     * 2. Se falhar, busca no Postgres via ClanDao.
     * 3. Salva no Redis e retorna.
     */
    fun getClan(id: Int): ClanEntity? {
        return getOrFetch(id) { dbId ->
            clanDao.findById(dbId)
        }
    }

    fun getClans(page: Int, pageSize: Int = 45): List<ClanEntity> {
//        println("Buscando clãs - Página: $page, PageSize: $pageSize")
        // Se o Redis retornar null (ou falhar), executa o bloco do DAO
        val result = pageCache.getOrFetch(page) { pageNum ->
            try {
                val offset = (pageNum - 1) * pageSize
//                println("Cache miss - Buscando do banco de dados (offset: $offset, limit: $pageSize)")
                val clans = clanDao.findWithPage(offset, pageSize)
//                println("Retornados ${clans.size} clãs do banco de dados")
                clans
            } catch (e: Exception) {
//                println("ERRO CRÍTICO NO DAO/JDBI: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
//        println("Retornando ${result.size} clãs")
        return result
    }

    fun update(clan: ClanEntity) {
        if (clan.id == null) return
        invalidate(clan.id)
    }
}