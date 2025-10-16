package org.quintilis.factions.managers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import java.sql.Connection
import java.sql.SQLException

object DatabaseManager {
    private var dataSource: HikariDataSource? = null
    lateinit var jdbi: Jdbi

    fun connect() {
        if(dataSource != null && !dataSource!!.isClosed) {
            return
        }

        val host = ConfigManager.getHost()
        val port = ConfigManager.getPort()
        val dbName = ConfigManager.getDatabaseName()
        val user = ConfigManager.getUsername()
        val pass = ConfigManager.getPassword()

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"
            username = user
            password = pass
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        this.dataSource = HikariDataSource(config)

        this.jdbi = Jdbi.create(dataSource).apply {
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

}