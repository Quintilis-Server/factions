package org.quintilis.factions.entities.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class TableName(val name: String)

