package org.quintilis.factions.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.BaseGui
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.Material
import org.bukkit.entity.Player
import java.text.MessageFormat

abstract class BaseGUI(
    val player: Player,
    val titleKey: String,
    val rows: Int = 6,
    val pageSize: Int = 45,
    val parent: BaseGui? = null
) {
    protected val mm = MiniMessage.miniMessage()

    val gui = Gui.paginated()
        .title(Component.translatable(titleKey))
        .rows(rows)
        .pageSize(pageSize)
        .disableAllInteractions()
        .create()

    init{
        this.setupLayout()
    }

    protected fun trans(key: String, vararg resolvers: TagResolver): Component {
        // Get the translation string from the resource bundle
        val bundle = java.util.ResourceBundle.getBundle("translations.factions", player.locale())
        val template = bundle.getString(key)
        
        // Use MiniMessage to deserialize with tag resolvers
        return mm.deserialize(template, *resolvers)
    }

    protected fun transLore(key: String, vararg resolvers: TagResolver): List<Component> {
        // Get the translation string from the resource bundle
        val bundle = java.util.ResourceBundle.getBundle("translations.factions", player.locale())
        val template = bundle.getString(key)
        
        println("DEBUG - template: '$template'")
        
        // Split by <newline> tag first
        val lines = template.split("<newline>")
        
        println("DEBUG - split into ${lines.size} lines")
        
        // Deserialize each line with tag resolvers
        return lines.map { line ->
            val trimmed = line.trim()
            println("DEBUG - processing line: '$trimmed'")
            mm.deserialize(trimmed, *resolvers)
        }
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