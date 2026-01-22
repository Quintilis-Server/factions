package org.quintilis.factions.item

import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.util.Keys

fun ItemStack.isNexusItem(): Boolean{
    val meta = this.itemMeta ?: return false
    return meta.persistentDataContainer.has(Keys.NEXUS_ITEM, PersistentDataType.STRING)
}

fun ItemStack.setGlowing(glowing: Boolean): ItemStack {
    val meta = this.itemMeta ?: return this

    if (glowing) {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
    }else{
        meta.removeEnchant(Enchantment.UNBREAKING)
        meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
    }
    this.itemMeta = meta
    return this
}