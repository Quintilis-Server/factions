package org.quintilis.factions.factory

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.quintilis.factions.enums.CoreType
import kotlin.random.Random

object CoreLootFactory {
    fun getDestructionLoot(type: CoreType): List<ItemStack> {
        val loot = mutableListOf<ItemStack>()

        when (type) {
            CoreType.NEXUS -> {
                // Loot pesado para quem destrói o coração do clã
                loot.add(ItemStack(Material.NETHERITE_INGOT, Random.nextInt(2, 5)))
                loot.add(ItemStack(Material.DIAMOND, Random.nextInt(16, 33)))
                loot.add(ItemStack(Material.GOLD_INGOT, Random.nextInt(32, 65)))
                loot.add(ItemStack(Material.GLOWSTONE, Random.nextInt(32, 65)))
            }
            CoreType.SUB_CORE -> {
                // Loot moderado para territórios periféricos
                loot.add(ItemStack(Material.DIAMOND, Random.nextInt(4, 13)))
                loot.add(ItemStack(Material.GOLD_INGOT, Random.nextInt(16, 33)))
                loot.add(ItemStack(Material.GLOWSTONE, Random.nextInt(16, 33)))
                loot.add(ItemStack(Material.IRON_INGOT, Random.nextInt(32, 65)))
            }
        }

        // Adiciona uma chance de 20% de vir algo bônus, como uma Maçã Dourada
        if (Random.nextDouble() < 0.20) {
            loot.add(ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1))
        }

        return loot
    }
}