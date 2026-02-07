package org.quintilis.factions.dao

import org.jdbi.v3.sqlobject.SqlObject
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.Define
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.entities.BaseEntity
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

//    fun getEntityClass(): Class<T> {
//        throw UnsupportedOperationException("A DAO filha deve implementar o método getEntityClass()!")
//    }
//
//    // --- MÉTODOS COM LÓGICA (Default Methods) ---
//    // O JDBI ignora métodos com corpo, então ele não tenta rodar SQL aqui.
//
//    fun getTableName(): String {
//        // Pegamos a classe via o método abstrato
//        val clazz = getEntityClass().kotlin
//        return clazz.findAnnotation<TableName>()?.name
//            ?: throw IllegalArgumentException("A classe ${clazz.simpleName} não tem @TableName")
//    }
//
//    fun getPkColumnName(): String {
//        val clazz = getEntityClass().kotlin
//        val pkProp = clazz.memberProperties.find { it.hasAnnotation<PrimaryKey>() }
//            ?: clazz.memberProperties.find { it.name == "id" }
//            ?: throw IllegalArgumentException("PK não encontrada em ${clazz.simpleName}")
//
//        return pkProp.findAnnotation<Column>()?.name ?: pkProp.name
//    }

    @SqlQuery("SELECT * FROM <table_name>")
    fun findAllDynamic(@Define("table_name") tableName: String): List<T>

    @SqlQuery("SELECT * FROM <table_name> WHERE <pk_col> = :id")
    fun findByIdDynamic(
        @Define("table_name") tableName: String,
        @Define("pk_col") pkCol: String,
        @Bind("id") id: ID
    ): T?
}