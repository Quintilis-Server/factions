package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.SqlObject
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.Define
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.economy.entities.annotations.Column
import org.quintilis.economy.entities.annotations.PrimaryKey
import org.quintilis.economy.entities.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Classe generica para as Dao, herda do `SqlObject`
 * Tem funções padrão de todas as DAOs
 * @param T recebe `BaseEntity`
 * @param ID o tipo de PrimaryKey do `T`
 */
interface BaseDao<T: BaseEntity, ID>: SqlObject {

    val entityClass: KClass<T>

    fun getTableName(): String {
        return entityClass.findAnnotation<TableName>()?.name
            ?: throw IllegalArgumentException("A classe ${entityClass.simpleName} não tem @TableName")
    }

    fun getPkColumnName(): String {
        val pkProp = entityClass.memberProperties.find { it.hasAnnotation<PrimaryKey>() }
            ?: entityClass.memberProperties.find { it.name == "id" } // Fallback para "id"
            ?: throw IllegalArgumentException("Não foi possível achar a PK de ${entityClass.simpleName}")

        return pkProp.findAnnotation<Column>()?.name ?: pkProp.name
    }

    @SqlQuery("SELECT * FROM <table_name>")
    fun findAllInternal(@Define("table_name") tableName: String): List<T>

    @SqlQuery("SELECT * FROM <table_name> WHERE <pk_col> = :id")
    fun findByIdInternal(
        @Define("table_name") tableName: String,
        @Define("pk_col") pkCol: String,
        @Bind("id") id: ID // O JDBI sabe lidar com Int, UUID, String aqui
    ): T?

    fun findAll(): List<T> {
        return findAllInternal(getTableName())
    }

    fun findById(id: ID): T? {
        return findByIdInternal(getTableName(), getPkColumnName(), id)
    }
}