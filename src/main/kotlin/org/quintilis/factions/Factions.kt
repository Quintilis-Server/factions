package org.quintilis.factions

import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.annotations.AutoRegister
import org.quintilis.factions.annotations.AutoTask
import org.quintilis.factions.commands.BaseCommand
import org.quintilis.factions.commands.clan.ClanCommand
import org.quintilis.factions.util.ClassScanner
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.DatabaseManager
import org.quintilis.factions.managers.RedisManager
import org.quintilis.factions.managers.TranslationManager
import org.quintilis.factions.services.CoreService
import org.quintilis.factions.services.FactionsServices
import org.quintilis.factions.util.Keys
import fr.skytasul.glowingentities.GlowingEntities
import org.bukkit.Bukkit
import org.quintilis.factions.commands.home.HomeCommand
import org.quintilis.factions.managers.QuintilisScheduler
import org.quintilis.factions.managers.QuintilisScheduler.isFolia
import org.quintilis.factions.placeholders.FactionsLangExpansion

class Factions : JavaPlugin() {

    override fun onEnable() {
        this.saveDefaultConfig()

        ConfigManager.initialize(this.config, this)

        try {
            logger.info("Conectando ao banco de dados PostgreSQL...")
            DatabaseManager.connect(logger, ConfigManager)
            logger.info("Conexão com o banco de dados estabelecida com sucesso!")
        } catch (e: Exception) {
            logger.severe("FALHA AO CONECTAR COM O BANCO DE DADOS! Desabilitando o plugin...")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }

        try{
            logger.info("Conectando ao banco de dados Redis...")
            RedisManager.connect(ConfigManager)
            RedisManager.flushDatabase()
            logger.info("Conexão com o banco de dados REDIS estabelecida com sucesso!")
        }catch (e: Exception){
            logger.severe("FALHA AO CONECTAR COM O REDIS! Desabilitando o plugin...")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
        FactionsServices.init(this)
        Keys.load(this)

        this.registerEvents()

        this.registerTasks()

        this.registerCommands()

//        GlowingEntities(this)

        this.registerTranslations()

        // Verificação do FancyNpcs
        if (server.pluginManager.getPlugin("FancyNpcs") != null && server.pluginManager.isPluginEnabled("FancyNpcs")) {
            logger.info("FancyNpcs detectado. Sistema de NPCs de AntiCore pronto.")
        } else {
            logger.severe("FancyNpcs NÃO ENCONTRADO! NPCs como o de AntiCore não funcionarão.")
            server.pluginManager.disablePlugin(this)
            return
        }
    }
    private fun registerTranslations() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            FactionsLangExpansion(
                "factions",
                "Quintilis",
                "1.0",
            ).register()
            logger.info("PlaceholderAPI conectado! Traduções dinâmicas ativadas.")
        }
        TranslationManager.registerTranslations(this, "factions")
    }

    private fun registerCommands(){
        fun printName(command: BaseCommand){
            logger.info("Registering ${command.name} commands")
            command.commands.forEach { command ->
                logger.info("Registering command: $command")
                if(command.subCommands != null){
                    command.subCommands!!.forEach { subCommand ->
                        logger.info("Registering subCommand: $subCommand")
                    }
                }
            }
        }
        val coreService = CoreService()
        val commands = listOf(ClanCommand(coreService, this), HomeCommand(this));
        this.server.commandMap.registerAll("factions", commands)
        commands.forEach {
            printName(it)
        }
    }

    private fun registerTasks() {
        logger.info("Registering tasks")
        val classes = ClassScanner.findClasses<Runnable, AutoTask>(
            this,
            "org.quintilis.factions"
        )
        classes.forEach { clazz ->
            try{
                val annotation = clazz.getAnnotation(AutoTask::class.java)

                val interval = if (annotation.configPath.isNotEmpty()) {
                    // Tenta pegar da config. Se não existir (retornar 0 ou nulo), usa o period da anotação
                    val fromConfig = config.getLong(annotation.configPath, 0L)
                    if (fromConfig > 0L) fromConfig else annotation.period
                } else {
                    annotation.period
                }

                val taskInstance = try{
                    clazz.getConstructor(Factions::class.java).newInstance(this)
                } catch (e: NoSuchMethodException){
                    clazz.getConstructor().newInstance()
                }

                QuintilisScheduler.runTimer(
                    this,
                    taskInstance,
                    annotation.delay,
                    interval,
                    annotation.async
                )

                logger.info("Task registrada automaticamente: ${clazz.simpleName}")
            }catch (e: Exception){
                logger.severe("Falha ao registrar task: ${clazz.simpleName}")
                e.printStackTrace()
            }
        }
    }

    private fun registerEvents() {
        val classes: List<Class<Listener>> = ClassScanner.findClasses<Listener, AutoRegister>(
            this,
            "org.quintilis.factions",
        )

        classes.forEach { clazz ->
            val listener = try {
                // Agora ele procura pelo construtor genérico JavaPlugin
                clazz.getConstructor(JavaPlugin::class.java).newInstance(this)
            } catch (e: NoSuchMethodException) {
                try {
                    // Tenta achar o construtor exato (Factions) como fallback
                    clazz.getConstructor(Factions::class.java).newInstance(this)
                } catch (e2: NoSuchMethodException) {
                    try {
                        // Se falhar os dois, tenta o construtor vazio ()
                        clazz.getConstructor().newInstance()
                    } catch (e3: Exception) {
                        logger.severe("Não foi possível registrar o listener ${clazz.simpleName}. Verifique os construtores.")
                        return@forEach
                    }
                }
            }

            server.pluginManager.registerEvents(listener, this)
            logger.info("Listener registrado: ${clazz.simpleName}")
        }
    }

    override fun onDisable() {
        DatabaseManager.close()
    }
}
