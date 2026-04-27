package org.quintilis.factions.extensions

import org.bukkit.Registry
import net.kyori.adventure.sound.Sound as KyoriSound
import org.bukkit.Sound as BukkitSound

fun BukkitSound.asKyori(
    source: KyoriSound.Source = KyoriSound.Source.PLAYER,
    volume: Float = 1f,
    pitch: Float = 1f
): KyoriSound {
    val namespacedKey = Registry.SOUNDS.getKey(this)
        ?: throw IllegalArgumentException("O som $this não possui uma chave registrada!")

    // Como NamespacedKey já implementa a interface Key do Kyori,
    // podemos passar direto para o método sound()
    return KyoriSound.sound(namespacedKey, source, volume, pitch)
}