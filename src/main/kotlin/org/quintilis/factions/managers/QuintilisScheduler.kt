package org.quintilis.factions.managers

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin

object QuintilisScheduler {
    val isFolia: Boolean = try {
        Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler")
        true
    }catch (e: ClassNotFoundException) {
        println(e)
        false
    }

    /**
     * Executa uma tarefa na thread da região da localização fornecida.
     */
    fun runAtLocation(plugin: JavaPlugin, location: Location, runnable: Runnable) {
        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, location, runnable)
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable)
        }
    }

    /**
     * Executa uma tarefa com delay na thread da região.
     */
    fun runDelayedAtLocation(plugin: JavaPlugin, location: Location, runnable: Runnable, delayTicks: Long) {
        if (isFolia) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, { _ -> runnable.run() }, delayTicks)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks)
        }
    }

    /**
     * Executa uma tarefa global (ideal para banco de dados ou lógica que não mexe em blocos).
     */
    fun runGlobal(plugin: JavaPlugin, runnable: Runnable) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable)
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable)
        }
    }

    /**
     * Registra uma tarefa repetitiva (Timer).
     * No Folia usa o GlobalRegionScheduler, no Bukkit usa o Scheduler tradicional.
     */
    fun runTimer(plugin: JavaPlugin, runnable: Runnable, delay: Long, period: Long, async: Boolean): Any {
        println(isFolia)
        return if (isFolia) {
            // Folia: GlobalRegionScheduler para loops de tasks
            Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, { _ -> runnable.run() }, delay.coerceAtLeast(1L), period)
        } else {
            // Bukkit/Paper tradicional
            if (async) {
                Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period)
            } else {
                Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period)
            }
        }
    }

    /**
     * Executa uma tarefa na thread da entidade (essencial para Jogadores no Folia).
     */
    fun runAtEntity(plugin: JavaPlugin, entity: Entity, runnable: Runnable) {
        if (isFolia) {
            // No Folia, entidades têm seu próprio scheduler
            entity.scheduler.execute(plugin, runnable, null, 0L)
        } else {
            // No Paper, usamos a thread principal
            Bukkit.getScheduler().runTask(plugin, runnable)
        }
    }
}