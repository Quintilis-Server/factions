package org.quintilis.factions.entities.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class TableName(val name: String)
