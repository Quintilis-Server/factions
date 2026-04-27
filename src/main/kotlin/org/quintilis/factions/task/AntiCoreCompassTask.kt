package org.quintilis.factions.task

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.quintilis.factions.annotations.AutoTask
import org.quintilis.factions.extensions.asKyori
import org.quintilis.factions.extensions.getClan
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.TranslationManager
import org.quintilis.factions.services.FactionsServices.antiCoreCache
import org.quintilis.factions.util.Keys

@AutoTask(period = 20L)
class AntiCoreCompassTask: BukkitRunnable() {
    override fun run() {
        val now = System.currentTimeMillis()
        val durationMs = ConfigManager.getAnticoreCompassExpiry() * 1000

        for(player in Bukkit.getOnlinePlayers()) {
            val playerClan = player.getClan() ?: continue
            val inventory = player.inventory

            inventory.contents.forEachIndexed { index, item ->
                if(item == null || item.type != Material.COMPASS) return@forEachIndexed

                val meta = item.itemMeta as? CompassMeta ?: return@forEachIndexed

                val pdc = meta.persistentDataContainer
                if(!pdc.has(Keys.ANTI_CORE_COMPASS_EXPIRY, PersistentDataType.LONG)) return@forEachIndexed

                var expiry = pdc.get(Keys.ANTI_CORE_COMPASS_EXPIRY, PersistentDataType.LONG) ?: -1

                if(expiry == -1L) {
                    if (item == inventory.itemInMainHand) {
                        val targetsClan = antiCoreCache.findAllAttackingClan(playerClan)

                        if(targetsClan.isNotEmpty()) {
                            expiry = now + durationMs
                            pdc.set(Keys.ANTI_CORE_COMPASS_EXPIRY, PersistentDataType.LONG, expiry)

                            item.itemMeta = meta

                            player.playSound(
                                Sound.BLOCK_BEACON_ACTIVATE.asKyori(pitch = 1.5f)
                            )
                            player.sendActionBar(TranslationManager.render("anticore.compass.activate"))
                        }
                    }
                    return@forEachIndexed
                }

                if(now> expiry) {
                    inventory.remove(item)
                    player.playSound(
                        Sound.ENTITY_ITEM_BREAK.asKyori()
                    )
                    player.sendActionBar(TranslationManager.render("anticore.compass.expired", player.locale()))
                    return@forEachIndexed
                }

                if (item == inventory.itemInMainHand) {
                    updateCompassTarget(player, item, meta, expiry - now, playerClan.id!!)
                }
            }
        }
    }

    private fun updateCompassTarget(player: Player, item: ItemStack, meta: CompassMeta, timeLeftMs: Long, clanId: Int) {
        val playerLoc = player.location
        val threats = antiCoreCache.findAllAttackingClan(clanId)

        val nearest = threats
            .filter { it.worldUuid == playerLoc.world.uid }
            .minByOrNull { it.getLocation()?.distanceSquared(playerLoc) ?: Double.MAX_VALUE }

        nearest?.getLocation()?.let { targetLoc ->
            meta.lodestone = targetLoc
            meta.isLodestoneTracked = false
            item.itemMeta = meta

            val dist = Math.sqrt(targetLoc.distanceSquared(playerLoc)).toInt()
            val secondsLeft = timeLeftMs / 1000

            // Aqui você pode usar o render com placeholders se tiver configurado
            player.sendActionBar(TranslationManager.render(
                "antiCore.compass.message",
                player.locale(),
                Placeholder.unparsed("dist", dist.toString()),
                Placeholder.unparsed("seconds_left", secondsLeft.toString()),
            ))
        }
    }
}