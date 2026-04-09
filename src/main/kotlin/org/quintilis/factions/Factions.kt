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

class Factions : JavaPlugin() {

    override fun onEnable() {
        this.saveDefaultConfig()

        ConfigManager.initialize(this.config, this)

        try {
            logger.info("Conectando ao banco de dados PostgreSQL...")
            DatabaseManager.connect(logger)
            logger.info("Conexão com o banco de dados estabelecida com sucesso!")
        } catch (e: Exception) {
            logger.severe("FALHA AO CONECTAR COM O BANCO DE DADOS! Desabilitando o plugin...")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }

        try{
            logger.info("Conectando ao banco de dados Redis...")
            RedisManager.connect()
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

        GlowingEntities(this)

        TranslationManager.registerTranslations(this)

        if (server.pluginManager.getPlugin("Citizens") != null && server.pluginManager.isPluginEnabled("Citizens")) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(
                net.citizensnpcs.api.trait.TraitInfo.create(org.quintilis.factions.traits.AntiCoreTrait::class.java).withName("anticore")
            )
            logger.info("Citizens detectado. AntiCoreTrait registrado com sucesso.")
        } else {
            logger.warning("Citizens não foi encontrado ou não está ativado. NPCs como o de AntiCore não funcionarão.")
        }
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
        val coreService = CoreService(this)
        val commands = listOf(ClanCommand(coreService));
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

                val taskInstance = try{
                    clazz.getConstructor(Factions::class.java).newInstance(this)
                } catch (e: NoSuchMethodException){
                    clazz.getConstructor().newInstance()
                }

                if(annotation.async){
                    server.scheduler.runTaskTimerAsynchronously(
                        this,
                        taskInstance,
                        annotation.delay,
                        annotation.period
                    )
                } else {
                    server.scheduler.runTaskTimer(
                        this,
                        taskInstance,
                        annotation.delay,
                        annotation.period
                    )
                }
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
                // 1. Tenta achar o construtor que pede (Factions)
                clazz.getConstructor(Factions::class.java).newInstance(this)
            } catch (e: NoSuchMethodException) {
                try {
                    // 2. Se falhar, tenta o construtor vazio ()
                    clazz.getConstructor().newInstance()
                } catch (e2: Exception) {
                    // Se falhar os dois, avisa no console
                    logger.severe("Não foi possível registrar o listener ${clazz.simpleName}. Verifique os construtores.")
                    e2.printStackTrace()
                    return@forEach
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
