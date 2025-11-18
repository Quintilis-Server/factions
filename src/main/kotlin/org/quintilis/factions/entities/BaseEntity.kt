package org.quintilis.factions.entities

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

private val KProperty1<*, *>.columnName: String
    get() = this.findAnnotation<Column>()?.name ?: this.name

abstract class BaseEntity {
    val tableName: String = this::class.findAnnotation<TableName>()?.name
        ?: throw IllegalArgumentException("A classe ${this::class.simpleName} não tem @TableName")

    val primaryKeyProperty = this::class.memberProperties
        .find { it.hasAnnotation<PrimaryKey>() }
        ?: this::class.memberProperties.find { it.name == "id"}
        ?: throw IllegalArgumentException("A classe ${this::class.simpleName} não tem @PrimaryKey")

    val primaryKeyColumnName: String = primaryKeyProperty.columnName



    fun <T: BaseEntity> save(): T{
        // Pega todas as propriedades da class
        val pkValue = (primaryKeyProperty as KProperty1<BaseEntity, *>).get(this)
        val properties = this::class.primaryConstructor?.parameters
            ?.mapNotNull { param ->
                this::class.memberProperties.find { prop -> prop.name == param.name && !prop.hasAnnotation<Transient>() }
            }
            ?.filter { prop ->
                // SE o valor da PK for null, REMOVA a PK da lista de propriedades do INSERT
                if (pkValue == null && prop.name == primaryKeyProperty.name) {
                    false // Exclui a PK
                } else {
                    true // Inclui todas as outras (incluindo UUIDs!)
                }
            }
            ?: emptyList()
        // Transforma para colunas sql
        val columns = properties.joinToString(", ") { it.columnName }
        // Transforma para tipos nomeados do jdbi
        val namedParams = properties.joinToString(", ") { ":${it.name}" }
        // cria o sql
        val updateSet = properties.filter { it.name != primaryKeyProperty.name }
            .joinToString(", ") { "${it.columnName} = :${it.name}" }

        val sql = """
            INSERT INTO $tableName ($columns)
            VALUES ($namedParams)
            ON CONFLICT ($primaryKeyColumnName) DO UPDATE SET
            $updateSet
            RETURNING *
        """.trimIndent()

        return DatabaseManager.jdbi.inTransaction<T, Exception> { handle ->
            val update = handle.createUpdate(sql)

            properties.forEach { prop ->
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