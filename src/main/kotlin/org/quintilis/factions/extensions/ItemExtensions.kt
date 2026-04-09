package org.quintilis.factions.extensions

import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.quintilis.factions.util.Keys

fun ItemStack.isNexusItem(): Boolean{
    val meta = itemMeta ?: return false
    val pdc = meta.persistentDataContainer

    // Checa primeiro se tem como INTEGER (o novo padrão)
    if (pdc.has(Keys.NEXUS_ITEM, PersistentDataType.INTEGER)) {
        return true
    }

    // Fallback de segurança: Caso você tenha itens antigos soltos pelo mapa
    // salvos como STRING, ele ainda vai reconhecer.
    return pdc.has(Keys.NEXUS_ITEM, PersistentDataType.STRING)
}

fun ItemStack.isCoreItem(): Boolean{
    val meta = this.itemMeta ?: return false
    val pdc = meta.persistentDataContainer
    if (pdc.has(Keys.CORE_ITEM, PersistentDataType.INTEGER)) {
        return true
    }
    return pdc.has(Keys.CORE_ITEM, PersistentDataType.STRING)
}

fun ItemStack.isAntiCore(): Boolean{
    val meta = this.itemMeta ?: return false
    val pdc = meta.persistentDataContainer
    if (pdc.has(Keys.ANTI_CORE_ITEM, PersistentDataType.INTEGER)) {
        return true
    }
    return pdc.has(Keys.ANTI_CORE_ITEM, PersistentDataType.STRING)
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