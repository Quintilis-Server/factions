package org.quintilis.factions.entities

import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.annotations.Transient as CustomTransient
import org.quintilis.factions.managers.DatabaseManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.Transient
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Propriedade da classe para achar a anotação `@Column`.
 * Lógica:
 * 1. Pega a anotação.
 * 2. Se o nome na anotação não for nulo e nem vazio, usa ele.
 * 3. Caso contrário, usa o nome da variável.
 */
private val KProperty1<*, *>.columnName: String
    get() {
        val annotation = this.findAnnotation<Column>()
        // Se a anotação existir E o nome não estiver em branco, usa o nome da anotação.
        // O takeIf retorna null se a condição for falsa (se for string vazia).
        return annotation?.name?.takeIf { it.isNotBlank() } ?: this.name
    }
/**
 * Estrutura de dados para cachear a Reflexão da classe.
 * Evita rodar reflection toda vez que salvar e resolve o bug do Lazy null.
 */
private data class EntityMetadata(
    val tableName: String,
    val primaryKeyProperties: List<KProperty1<BaseEntity, *>>,
    val primaryKeyColumnNames: List<String>
)
/**
 * Classe abstrata que outras entidades herdam
 * Essa classe precisa da `@TableName`, `@Column` e pelo menos uma `@PrimaryKey`
 */
abstract class BaseEntity {
    companion object {
        private val metadataCache = ConcurrentHashMap<KClass<*>, EntityMetadata>()

        private fun getMetadata(clazz: KClass<*>): EntityMetadata {
            return metadataCache.computeIfAbsent(clazz) { kClass ->
                // 1. Table Name
                val tableName = kClass.findAnnotation<TableName>()?.name
                    ?: throw IllegalArgumentException("A classe ${kClass.simpleName} não tem @TableName")

                // 2. PK Properties
                @Suppress("UNCHECKED_CAST")
                val allProps = kClass.memberProperties as Collection<KProperty1<BaseEntity, *>>

                val pkProps = allProps.filter { it.hasAnnotation<PrimaryKey>() }
                    .ifEmpty {
                        // Fallback para "id"
                        val idProp = allProps.find { it.name == "id" }
                        if (idProp != null) listOf(idProp) else emptyList()
                    }

                // 3. PK Column Names
                val pkColNames = pkProps.map { it.columnName }

                EntityMetadata(tableName, pkProps, pkColNames)
            }
        }
    }

    /**
     * Salva a entidade na database
     * Ela da update se ele ja existir dentro da database, conforme o `@PrimaryKey`
     * ou ela insere se não existir ja
     * @param T É uma BaseEntity, preferencialmente precisa ser a propria classe, o cast é automático para essa função
     * @return o tipo genérico passado antes
     */
    @Transaction
    fun <T : BaseEntity> save(): T {
        val meta = getMetadata(this::class)
        val tableName = meta.tableName
        val primaryKeyProperties = meta.primaryKeyProperties
        val primaryKeyColumnNames = meta.primaryKeyColumnNames

        // 1. Verificação de Auto-Increment (Serial)
        val singlePk = primaryKeyProperties.singleOrNull()
        val isAutoIncrementInsert = singlePk != null && singlePk.get(this) == null

        // 2. Filtra quais propriedades vamos enviar para o banco
        // Nota: Reflection no memberProperties aqui é segura pois usa a instância atual
        val propertiesToSave = this::class.primaryConstructor?.parameters
            ?.mapNotNull { param ->
                this::class.memberProperties.find { prop -> prop.name == param.name && !prop.hasAnnotation<CustomTransient>() }
            }
            ?.filter { prop ->
                if (isAutoIncrementInsert && prop.name == singlePk!!.name) {
                    false
                } else {
                    true
                }
            } ?: emptyList()

        // 3. Montagem do SQL
        val columns = propertiesToSave.joinToString(", ") { it.columnName }
        val namedParams = propertiesToSave.joinToString(", ") { ":${it.name}" }

        val sql: String

        if (isAutoIncrementInsert) {
            // INSERT SIMPLES
            sql = """
                INSERT INTO $tableName ($columns) VALUES ($namedParams)
                RETURNING *
            """.trimIndent()
        } else {
            // UPSERT
            val conflictTarget = primaryKeyColumnNames.joinToString(", ")

            val updateSet = propertiesToSave
                .filter { prop ->
                    primaryKeyProperties.none { pk -> pk.name == prop.name }
                }
                .joinToString(", ") { "${it.columnName} = :${it.name}" }

            val doAction = if (updateSet.isNotEmpty()) "DO UPDATE SET $updateSet" else "DO NOTHING"

            sql = """
                INSERT INTO $tableName ($columns) VALUES ($namedParams)
                ON CONFLICT ($conflictTarget) $doAction
                RETURNING *
            """.trimIndent()
        }

        // 4. Execução JDBI
        val savedEntity = DatabaseManager.jdbi.inTransaction<T, Exception> { handle ->
            val update = handle.createUpdate(sql)

            propertiesToSave.forEach { prop ->
                @Suppress("UNCHECKED_CAST")
                val typedProp = prop as KProperty1<BaseEntity, *>
                var value = typedProp.get(this)

                if (value is Enum<*>) {
                    value = value.name
                }

                update.bind(prop.name, value)
            }

            // Mapeamento seguro
            update.executeAndReturnGeneratedKeys()
                .mapTo(this::class.java as Class<T>)
                .one()
        }

        // 5. Atualiza Cache (Write-Through)
        EntityCacheRegistry.updateCache(savedEntity)

        return savedEntity
    }
}