package org.quintilis.factions.scoreboard

import me.catcoder.sidebar.ProtocolSidebar
import me.catcoder.sidebar.Sidebar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.quintilis.factions.Factions
import org.quintilis.factions.managers.TranslationManager
import org.quintilis.factions.services.FactionsServices.clanCache
import org.quintilis.factions.services.FactionsServices.playerCache
import java.util.UUID

object Scoreboard {
    private lateinit var plugin: Factions
    private val sidebars = mutableMapOf<UUID, Sidebar<Component>>()

    fun init(plugin: Factions) {
        this.plugin = plugin
    }

    fun setupScoreboard(player: Player) {
        val title = TranslationManager.render("scoreboard.sidebar.title", player.locale())
        val sidebar = ProtocolSidebar.newAdventureSidebar(title, plugin)

        sidebar.updateLinesPeriodically(20,20)

        val rawLines = TranslationManager.getRawList("scoreboard.sidebar.content", player.locale())
        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()

        for (rawLine in rawLines) {
            when (rawLine.trim()) {
                "<top_players>" -> {
                    for (i in 0 until 3) {
                        sidebar.addUpdatableLine { _: Player ->
                            val topPlayers = playerCache.findTopPlayers()

                            if (topPlayers.isEmpty() && i == 0) {
                                Component.text("  Nenhum jogador") // Mostra só na primeira linha
                            } else if (i < topPlayers.size) {
                                val pl = topPlayers[i]
                                Component.text("  ${i + 1}. ${pl.name}")
                            } else {
                                Component.empty() // Oculta as linhas que sobrarem
                            }
                        }
                    }
                }
                "<top_clans>" -> {
                    for (i in 0 until 3) {
                        sidebar.addUpdatableLine { playerContext: Player ->
                            val topClans = clanCache.findTopClans()

                            if (topClans.isEmpty() && i == 0) {
                                Component.text("  Nenhum clã")
                            } else if (i < topClans.size) {
                                val clan = topClans[i]

                                // 1. Renderiza a Tag do Clã se ela existir
                                val tagComponent = if (clan.tag != null) {
                                    TranslationManager.render(
                                        "scoreboard.clan.tag",
                                        playerContext.locale(),
                                        Placeholder.unparsed("tag", clan.tag)
                                    )
                                } else {
                                    Component.empty()
                                }

                                // 2. Constrói a linha unindo os Componentes
                                Component.text("  ${i + 1}. ")
                                    .append(tagComponent)
                                    .append(Component.text(clan.name))

                            } else {
                                Component.empty()
                            }
                        }
                    }
                }
                "" -> sidebar.addBlankLine()
                else -> sidebar.addLine(mm.deserialize(rawLine)) // linha estática
            }
        }

        sidebar.addViewer(player)
        sidebars[player.uniqueId] = sidebar
    }

    fun updateScoreboard(player: Player) {
        // só atualiza as linhas updatable, sem recriar nada
        sidebars[player.uniqueId]?.updateAllLines()
    }

    private fun buildSidebarLines(player: Player, sidebar: Sidebar<Component>) {
        val clanLines = clanCache.findTopClans()
            .mapIndexed { i, clan ->
                val tag = if (clan.tag != null) "(${clan.tag}) " else ""
                Component.text("${i + 1}. $tag${clan.name}")
            }

        val playerLines = playerCache.findTopPlayers()
            .mapIndexed { i, p -> Component.text("${i + 1}. ${p.name}") }

        val rawLines = TranslationManager.getRawList("scoreboard.sidebar.content", player.locale())

        for (rawLine in rawLines) {
            when {
                rawLine.trim() == "<top_players>" -> {
                    if (playerLines.isEmpty()) {
                        sidebar.addLine(Component.text("Nenhum jogador"))
                    } else {
                        playerLines.forEach { sidebar.addLine(it) }
                    }
                }
                rawLine.trim() == "<top_clans>" -> {
                    if (clanLines.isEmpty()) {
                        sidebar.addLine(Component.text("Nenhum clã"))
                    } else {
                        clanLines.forEach { sidebar.addLine(it) }
                    }
                }
                rawLine.isEmpty() -> sidebar.addBlankLine()
                else -> {
                    val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    sidebar.addLine(mm.deserialize(rawLine))
                }
            }
        }
    }
}