package org.quintilis.factions.cache

import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.dao.BaseDao
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.CacheSync
import org.quintilis.factions.entities.EntityCacheRegistry
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

abstract class AbstractDaoCache<D: BaseDao<E, K>, E: BaseEntity, K>(
    protected val dao: D,
    prefix: String,
    ttl: Long,
    classType: Class<E>,
    plugin: JavaPlugin
): JsonCache<K, E>(prefix, ttl, classType, plugin), CacheSync {

    protected val tableName: String
    protected val pkColName: String

    init {
        // Lógica de Reflexão movida para o INIT da Classe Abstrata (Roda 1 vez na inicialização)
        val kClass = classType.kotlin

        tableName = kClass.findAnnotation<TableName>()?.name
            ?: throw IllegalArgumentException("A classe ${kClass.simpleName} não tem @TableName")

        val pkProp = kClass.memberProperties.find { it.hasAnnotation<PrimaryKey>() }
            ?: kClass.memberProperties.find { it.name == "id" }
            ?: throw IllegalArgumentException("PK não encontrada em ${kClass.simpleName}")

        val annotationName = pkProp.findAnnotation<Column>()?.name
        pkColName = if (!annotationName.isNullOrBlank()) {
            annotationName
        } else {
            pkProp.name // Se for vazio ou nulo, usa o nome da variável (ex: "id")
        }
        EntityCacheRegistry.register(classType.kotlin, this)
    }
    /**
     * Implementação padrão do findById.
     * Tenta Redis -> Se falhar, vai no DAO -> Salva no Redis -> Retorna.
     */
    open fun findById(id: K): E? {
        return getOrFetch(id) { dbId ->
            dao.findByIdDynamic(tableName, pkColName, dbId)
        }
    }

    // Você pode adicionar métodos utilitários de update aqui se quiser
    open fun invalidateAndReload(id: K): E? {
        invalidate(id)
        return findById(id)
    }

    private fun getIdFromEntity(entity: E): K {
        val kClass = entity::class
        val pkProp = kClass.memberProperties.firstOrNull { it.hasAnnotation<PrimaryKey>() }
            ?: kClass.memberProperties.firstOrNull { it.name == "id" }
            ?: throw IllegalStateException("PK não achada para cachear ${kClass.simpleName}")

        @Suppress("UNCHECKED_CAST")
        return (pkProp as KProperty1<E, K>).get(entity)
    }

    override fun updateCache(entity: BaseEntity) {
        try {
            // Conversão segura: BaseEntity -> E
            @Suppress("UNCHECKED_CAST")
            val castedEntity = entity as E

            // Descobre o ID da entidade (usando a lógica de PK)
            val id = getIdFromEntity(castedEntity)

            // Salva no Redis
            put(id, castedEntity)

        } catch (e: Exception) {
            e.printStackTrace() // Não para o servidor se o cache falhar
        }
    }
}