package org.quintilis.factions.task

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.quintilis.factions.annotations.AutoTask
import org.quintilis.factions.scoreboard.Scoreboard

@AutoTask(20L,20L)
class ScoreboardTask: BukkitRunnable() {
    override fun run() {
        for(player in Bukkit.getOnlinePlayers()) {
            Scoreboard.updateScoreboard(player)
        }
    }
}