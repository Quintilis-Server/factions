package org.quintilis.factions.util

import org.bukkit.NamespacedKey
import org.quintilis.factions.Factions

object Keys {
    lateinit var NEXUS_ITEM: NamespacedKey
    lateinit var CORE_ITEM: NamespacedKey

    fun load(plugin: Factions) {
        NEXUS_ITEM = NamespacedKey(plugin, "nexus_core_item")
        CORE_ITEM = NamespacedKey(plugin, "core_core_item")
    }
}