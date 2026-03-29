package org.quintilis.factions.managers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.quintilis.factions.Factions
//import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.quintilis.factions.dao.BaseDao
import java.sql.Connection
import java.sql.SQLException
import java.util.logging.Logger
import kotlin.reflect.KClass

object DatabaseManager {
    private var dataSource: HikariDataSource? = null
    lateinit var jdbi: Jdbi

    fun connect(logger: Logger) {
        if(dataSource != null && !dataSource!!.isClosed) {
            return
        }

        val host = ConfigManager.getHost()
        val port = ConfigManager.getPort()
        val dbName = ConfigManager.getDatabaseName()
        val user = ConfigManager.getUsername()
        val pass = ConfigManager.getPassword()

        val dbUrl = "jdbc:postgresql://$host:$port/$dbName"
        logger.info("Connecting to database: $dbUrl")
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = user
            password = pass
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        this.dataSource = HikariDataSource(config)

        try {
            logger.info("Initiating Flyway")

            val flyway = Flyway.configure(Factions::class.java.classLoader)
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load()

            val result = flyway.migrate()

            if (result.migrationsExecuted > 0) {
                logger.info("Flyway: ${result.migrationsExecuted} migrações aplicadas com sucesso!")
            } else {
                logger.info("Flyway: Banco de dados já está atualizado.")
            }
        }catch (e: Exception){
            logger.severe("Erro crítico ao executar o Flyway! O banco pode estar inconsistente.")
            e.printStackTrace()
        }

        this.jdbi = Jdbi.create(dataSource).apply {
            installPlugin(SqlObjectPlugin())
            installPlugin(KotlinPlugin())
        }
    }

    fun close(){
        dataSource?.close()
    }

    @Throws(SQLException::class)
    fun getConnection(): Connection? {
        return dataSource?.connection
    }

    fun <T : BaseDao<*, *>> getDAO(daoClass: KClass<T>): T {
        return this.jdbi.onDemand(daoClass.java)
    }

}