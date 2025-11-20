package org.quintilis.factions

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.commands.BaseCommand
import org.quintilis.factions.commands.clan.ClanCommand
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.DatabaseManager
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

class Factions : JavaPlugin() {

    override fun onEnable() {
        this.saveDefaultConfig()

        ConfigManager.initialize(this.config)

        try {
            logger.info("Conectando ao banco de dados PostgreSQL...")
            DatabaseManager.connect()
            logger.info("Conexão com o banco de dados estabelecida com sucesso!")
        } catch (e: Exception) {
            logger.severe("FALHA AO CONECTAR COM O BANCO DE DADOS! Desabilitando o plugin...")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }

        this.registerCommands()

        this.registerTranslations()
    }

    private fun registerCommands(){
        fun printName(command: BaseCommand){
            logger.info("Registering ${command.name} commands")
        }

        val commands = listOf(ClanCommand());
        this.server.commandMap.registerAll("economy", commands)
        commands.forEach {
            printName(it)
        }
    }

    private fun registerTranslations() {
        //todo fazer um sistema de array q pode setar quais traduções serão feitas
        val translationKey = Key.key("economy", "translations")

        val store = MiniMessageTranslationStore.create(translationKey)

        val english = Locale.US
        val portuguese = Locale.forLanguageTag("pt-BR")

        val bundlePath = "translations.economy"

        try {
            val bundleEN = ResourceBundle.getBundle(bundlePath, english)
            val bundlePT = ResourceBundle.getBundle(bundlePath, portuguese)

            store.registerAll(english, bundleEN, false)
            store.registerAll(portuguese, bundlePT, false)

        } catch (e: MissingResourceException) {
            logger.warning("NÃO FOI POSSÍVEL ENCONTRAR OS ARQUIVOS DE TRADUÇÃO NO JAR!")
            logger.warning("Verifique o caminho: $bundlePath")
            return
        }

        GlobalTranslator.translator().addSource(store)

        logger.info("Translation sources (en, pt_BR) registered successfully.")

        logger.info("Plugin ${this.name} successfully initiated")
    }

    override fun onDisable() {
        DatabaseManager.close()
    }
}
