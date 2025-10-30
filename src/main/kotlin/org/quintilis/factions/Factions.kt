package org.quintilis.factions

import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.commands.ClanCreateCommand
import org.quintilis.factions.entities.managers.ClaimManager
import org.quintilis.factions.entities.models.Clan
import org.quintilis.factions.listeners.BlockProtectionListener
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

            ClaimManager.loadClaims()

            val clan = Clan(name = "Leofoda", tag = "Leo")
            clan.save()

            server.pluginManager.registerEvents(BlockProtectionListener(), this)

            getCommand("clan")?.setExecutor(ClanCreateCommand())

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
