package org.quintilis.factions.entities

import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import org.quintilis.economy.entities.annotations.Transient
import org.quintilis.factions.managers.DatabaseManager
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Propriedade da classe para achar a anotação `@Column`
 */
private val KProperty1<*, *>.columnName: String
    get() = this.findAnnotation<Column>()?.name ?: this.name

/**
 * Classe abstrata que outras entidade herdam
 * Essa classe precisa da `@TableName`, `@Column` e pelo menos uma `@PrimaryKey`
 */
abstract class BaseEntity {
    val tableName: String = this::class.findAnnotation<TableName>()?.name
        ?: throw IllegalArgumentException("A classe ${this::class.simpleName} não tem @TableName")

    val primaryKeyProperties: List<KProperty1<*, *>> = this::class.memberProperties
        .filter { it.hasAnnotation<PrimaryKey>() }
        .ifEmpty { // Fallback para "id" se nenhuma @PrimaryKey for encontrada
            this::class.memberProperties.filter { it.name == "id" }
        }

    val primaryKeyColumnNames: List<String> = primaryKeyProperties.map { it.columnName }

    val primaryKeyPropertyNames: List<String> = primaryKeyProperties.map { it.name }

    /**
     * Salva a entidade na database
     * Ela da update se ele ja existir dentro da database, conforme o `@PrimaryKey`
     * ou ela insere se não existir ja
     * @param T É uma BaseEntity, preferencialmente precisa ser a propria classe, o cast é automático para essa função
     * @return o tipo genérico passado antes
     */
    @Transaction
    fun <T : BaseEntity> save(): T {

        val singlePkValue = if (primaryKeyProperties.size == 1) {
            (primaryKeyProperties.first() as KProperty1<BaseEntity, *>).get(this)
        } else {
            false
        }

        val properties = this::class.primaryConstructor?.parameters
            ?.mapNotNull { param ->
                this::class.memberProperties.find { prop -> prop.name == param.name && !prop.hasAnnotation<Transient>() }
            }
            ?.filter { prop ->
                if (singlePkValue == null && prop.name == primaryKeyProperties.firstOrNull()?.name) {
                    false
                } else {
                    true
                }
            }
            ?: emptyList()

        // Transforma para colunas sql
        val columns = properties.joinToString(", ") { it.columnName }
        // Transforma para tipos nomeados do jdbi
        val namedParams = properties.joinToString(", ") { ":${it.name}" }

        val updateSet = properties
            .filter { it.name !in primaryKeyPropertyNames }
            .joinToString(", ") { "${it.columnName} = :${it.name}" }

        val conflictColumns = primaryKeyColumnNames.joinToString(", ")

        val sql = """
            INSERT INTO $tableName ($columns)
            VALUES ($namedParams)
            ON CONFLICT ($conflictColumns) DO UPDATE SET
            $updateSet
            RETURNING *
        """.trimIndent()

        return DatabaseManager.jdbi.inTransaction<T, Exception> { handle ->
            val update = handle.createUpdate(sql)


            val allProperties = this::class.memberProperties
                .filter { !it.hasAnnotation<Transient>() }

            allProperties.forEach { prop ->
                @Suppress("UNCHECKED_CAST")
                val typedProp = prop as KProperty1<BaseEntity, *>
                val value = typedProp.get(this)
                update.bind(prop.name, value)
            }

            update.executeAndReturnGeneratedKeys()
                .mapTo(this.javaClass as Class<T>)
                .one()
        }
    }
}