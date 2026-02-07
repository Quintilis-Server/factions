package org.quintilis.factions.cache

import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.dao.BaseDao
import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.util.CacheSync
import org.quintilis.factions.util.EntityCacheRegistry
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

abstract class AbstractDaoCache<D: BaseDao<E, K>, E: BaseEntity, K>(
    protected val dao: D,
    prefix: String,
    ttl: Long,
    classType: Class<E>
): JsonCache<K, E>(prefix, ttl, classType), CacheSync {

    protected val tableName: String
    protected val pkColName: String

    init {
        val kClass = classType.kotlin

        tableName = kClass.findAnnotation<TableName>()?.name
            ?: throw IllegalArgumentException("A classe ${kClass.simpleName} não tem @TableName")

        val pkProp = kClass.memberProperties.find { it.hasAnnotation<PrimaryKey>() }
            ?: kClass.memberProperties.find { it.name == "id" }
            ?: throw IllegalArgumentException("PK não encontrada em ${kClass.simpleName}")

        pkColName = pkProp.findAnnotation<Column>()?.name ?: pkProp.name
        EntityCacheRegistry.register(classType.kotlin, this)
    }

    // ... métodos findById, etc ...

    // =========================================================================
    // IMPLEMENTAÇÃO DA SINCRONIZAÇÃO (Chamada pela BaseEntity)
    // =========================================================================
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

    // Helper para pegar o ID via Reflection (já que aqui é genérico)
    private fun getIdFromEntity(entity: E): K {
        val kClass = entity::class
        val pkProp = kClass.memberProperties.firstOrNull { it.hasAnnotation<PrimaryKey>() }
            ?: kClass.memberProperties.firstOrNull { it.name == "id" }
            ?: throw IllegalStateException("PK não achada para cachear ${kClass.simpleName}")

        @Suppress("UNCHECKED_CAST")
        return (pkProp as KProperty1<E, K>).get(entity)
    }
    /**
     * Implementação padrão do findById.
     * Tenta Redis -> Se falhar, vai no DAO -> Salva no Redis -> Retorna.
     */
    open fun findById(id: K): E? {
        return getOrFetch(id) { dbId ->
            // Chama o método dinâmico da DAO passando os metadados calculados
            dao.findByIdDynamic(tableName, pkColName, dbId)
        }
    }

    // Você pode adicionar métodos utilitários de update aqui se quiser
    open fun invalidateAndReload(id: K): E? {
        invalidate(id)
        return findById(id)
    }
}