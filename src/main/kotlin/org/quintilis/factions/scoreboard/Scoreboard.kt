package org.quintilis.factions.scoreboard

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.quintilis.factions.Factions
import org.quintilis.factions.managers.TranslationManager
import io.papermc.paper.scoreboard.numbers.NumberFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.quintilis.factions.services.FactionsServices.clanCache
import org.quintilis.factions.services.FactionsServices.playerCache

object Scoreboard {
    private lateinit var plugin: Factions

    fun init(plugin: Factions){
        this.plugin = plugin
    }

    fun setupScoreboard(player: Player){
        val manager = Bukkit.getScoreboardManager();
        val scoreboard = manager.newScoreboard;

        val objective = scoreboard.registerNewObjective(
            "sidebar",
            Criteria.DUMMY,
            TranslationManager.render("scoreboard.sidebar.title"),
        );

        objective.displaySlot = DisplaySlot.SIDEBAR

        player.scoreboard = scoreboard;

        updateScoreboard(player);
    }

    fun updateScoreboard(player: Player) {
        player.getScheduler().run(plugin, { _ ->
            val scoreboard = player.scoreboard
            val objective = scoreboard.getObjective("sidebar") ?: return@run

            // Limpa entradas antigas
            scoreboard.entries.forEach { scoreboard.resetScores(it) }

            // Remove o número do score para todas as linhas do objetivo
            objective.numberFormat(NumberFormat.blank())

            val clanNames = clanCache.findTopClans()
                .mapIndexed { index, clan ->
                    val tag = if (clan.tag != null) "(${clan.tag}) " else ""
                    Component.text("${index + 1}. $tag${clan.name}")
                }
                .let { list ->
                    Component.join(
                        JoinConfiguration.newlines(),
                        list
                    )
                }

            val playerNames = playerCache.findTopPlayers()
                .mapIndexed { index, player ->
                    Component.text("${index + 1}. ${player.name}")
                }
                .let { list ->
                    Component.join(
                        JoinConfiguration.newlines(),
                        list
                    )
                }

            val lines = TranslationManager.renderList(
                "scoreboard.sidebar.content",
                player.locale(),
                Placeholder.component("top_clans", clanNames),
                Placeholder.component("top_players", playerNames)
            )

            lines.forEachIndexed { index, component ->
                val scoreValue = lines.size - index

                val entry = "§$index"
                val score = objective.getScore(entry)
                score.score = scoreValue

                score.customName(component)
            }
        }, null)
    }
}