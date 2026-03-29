package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.SqlObject
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.Define
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
import java.util.concurrent.ConcurrentHashMap
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
    @SqlQuery("SELECT * FROM <table_name>")
    fun findAllDynamic(@Define("table_name") tableName: String): List<T>

    @SqlQuery("SELECT * FROM <table_name> WHERE <pk_col> = :id")
    fun findByIdDynamic(
        @Define("table_name") tableName: String,
        @Define("pk_col") pkCol: String,
        @Bind("id") id: ID
    ): T?
}