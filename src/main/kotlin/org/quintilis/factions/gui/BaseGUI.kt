package org.quintilis.factions.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.BaseGui
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.quintilis.factions.managers.TranslationManager
import java.text.MessageFormat
import kotlin.math.ceil

abstract class BaseGUI(
    val player: Player,
    val titleKey: String,
    val rows: Int = 6,
    val pageSize: Int = 45,
    val parent: BaseGui? = null,
    val plugin: JavaPlugin,
) {

    var currentPageIndex = 1

    protected val mm = MiniMessage.miniMessage()

    val gui = Gui.paginated()
        .title(Component.translatable(titleKey))
        .rows(rows)
        .pageSize(pageSize)
        .disableAllInteractions()
        .create()

    protected fun <T> loadPageData(
        page: Int,
        fetcher: () -> List<T>,       // Função que busca a lista no DB
        totalFetcher: () -> Int,      // Função que conta o total no DB
        itemMapper: (T) -> dev.triumphteam.gui.guis.GuiItem // Como transformar T em item da GUI
    ) {
        this.currentPageIndex = page
        gui.clearPageItems()

        Bukkit.getServer().globalRegionScheduler.execute(plugin) {
            val data = fetcher()
            val total = totalFetcher()
            val totalPages = ceil(total.toDouble() / pageSize).toInt()

            player.scheduler.execute(plugin, {
                data.forEach { entry ->
                    gui.addItem(itemMapper(entry))
                }

                updateNavigationButtons(totalPages, plugin, fetcher, totalFetcher, itemMapper)
                gui.update()
            }, null, 0L)
        }
    }

    private fun <T> updateNavigationButtons(
        totalPages: Int,
        plugin: JavaPlugin,
        fetcher: () -> List<T>,
        totalFetcher: () -> Int,
        itemMapper: (T) -> GuiItem
    ) {
        // Botão Voltar
        if (currentPageIndex > 1) {
            gui.setItem(rows, 3, ItemBuilder.from(Material.PAPER)
                .name(trans("gui.previous_page"))
                .asGuiItem { loadPageData(currentPageIndex - 1, fetcher, totalFetcher, itemMapper) })
        } else {
            gui.removeItem(rows, 3)
        }

        // Botão Próximo
        if (currentPageIndex < totalPages) {
            gui.setItem(rows, 7, ItemBuilder.from(Material.PAPER)
                .name(trans("gui.next_page"))
                .asGuiItem { loadPageData(currentPageIndex + 1, fetcher, totalFetcher, itemMapper) })
        } else {
            gui.removeItem(rows, 7)
        }
    }

    init{
        this.setupLayout()
    }

    protected fun trans(key: String, vararg resolvers: TagResolver): Component {
        val template = TranslationManager.getRawMessage(key, player.locale())
        
        // Use MiniMessage to deserialize with tag resolvers
        return mm.deserialize(template, *resolvers)
    }

    protected fun transLore(key: String, vararg resolvers: TagResolver): List<Component> {
        val template = TranslationManager.getRawMessage(key, player.locale())
        
        // Split by <newline> tag first
        val lines = template.split("<newline>")
        
        // Deserialize each line with tag resolvers
        return lines.map { line ->
            val trimmed = line.trim()
            mm.deserialize(trimmed, *resolvers)
        }
    }

    protected fun createItem(
        material: Material,
        nameKey: String,
        loreKey: String? = null,
        vararg resolvers: TagResolver
    ): ItemBuilder {
        val builder = ItemBuilder.from(material).name(trans(nameKey, *resolvers))
        if (loreKey != null) {
            builder.lore(transLore(loreKey, *resolvers))
        }
        return builder
    }

    private fun setupLayout() {
        val filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.empty())
            .asGuiItem()

        gui.filler.fillBottom(filler)

        gui.setItem(rows, 3, ItemBuilder.from(Material.PAPER)
            .name(trans("gui.previous_page"))
            .asGuiItem { gui.previous() }) // .previous() existe na PaginatedGui

        gui.setItem(rows, 7, ItemBuilder.from(Material.PAPER)
            .name(trans("gui.next_page"))
            .asGuiItem { gui.next() }) // .next() existe na PaginatedGui

        if (parent != null) {
            gui.setItem(rows, 5, ItemBuilder.from(Material.ARROW)
                .name(trans("gui.back"))
                .asGuiItem {
                    parent.open(player)
                })
        } else {
            gui.setItem(rows, 5, ItemBuilder.from(Material.BARRIER)
                .name(trans("gui.close"))
                .asGuiItem {
                    gui.close(player)
                })
        }
    }

    abstract fun loadItems()

    fun open() {
        loadItems()
        gui.open(player)
    }
}