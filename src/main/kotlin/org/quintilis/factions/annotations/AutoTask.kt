package org.quintilis.factions.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoTask(
    val delay: Long = 0L,
    val period: Long,
    val async: Boolean = true,
    val configPath: String = ""
)
