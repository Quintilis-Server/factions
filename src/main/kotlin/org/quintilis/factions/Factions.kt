package org.quintilis.factions

import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.entities.models.Claim
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.DatabaseManager

class Factions : JavaPlugin() {

    override fun onEnable() {
        this.saveDefaultConfig()

        ConfigManager.initialize(this.config)

        try {
            logger.info("Conectando ao banco de dados PostgreSQL...")
            DatabaseManager.connect()
            logger.info("Conexão com o banco de dados estabelecida com sucesso!")

            val claim = Claim(
                id = 1,
                world = "world",
                chunkX = 10,
                chunkZ = 20,
                clanId = 3
            )

            claim.save()

        } catch (e: Exception) {
            logger.severe("FALHA AO CONECTAR COM O BANCO DE DADOS! Desabilitando o plugin...")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
    }

    override fun onDisable() {
        DatabaseManager.close()
    }
}
