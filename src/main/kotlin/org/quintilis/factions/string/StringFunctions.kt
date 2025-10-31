package org.quintilis.factions.string

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

fun String.bold(): Component =
    Component.text(this)
        .decoration(TextDecoration.BOLD, true)
        .color(NamedTextColor.WHITE)

fun String.italic(): Component =
    Component.text(this)
        .decoration(TextDecoration.ITALIC, true)
        .color(NamedTextColor.WHITE)

fun String.color(color: NamedTextColor): Component =
    Component.text(this)
        .color(color)