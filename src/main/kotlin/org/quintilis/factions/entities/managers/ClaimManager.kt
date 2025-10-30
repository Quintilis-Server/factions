package org.quintilis.factions.entities.managers

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.quintilis.factions.entities.models.Claim
import org.quintilis.factions.managers.DatabaseManager

object ClaimManager {
    private val claims = mutableListOf<Claim>()

    fun loadClaims() {
        DatabaseManager.jdbi.useHandle<Exception> { handle ->
            val loaded = handle.createQuery("SELECT * FROM chunk_claims")
                .mapTo(Claim::class.java)
                .list()

            claims.clear()
            claims.addAll(loaded)
            Bukkit.getLogger().info("[Factions] ${claims.size} loaded claims!")
        }
    }

    fun getClaim(chunk: Chunk): Claim? {
        return claims.find {
            it.world == chunk.world.name &&
                    it.chunkX == chunk.x &&
                    it.chunkZ == chunk.z
        }
    }

    fun getClaim(world: String, x: Int, z: Int): Claim? {
        return claims.find { it.world == world && it.chunkX == x && it.chunkZ == z }
    }

    fun removeClaim(chunk: Chunk) {
        val claim = getClaim(chunk)
        if (claim != null) {
            DatabaseManager.jdbi.useHandle<Exception> { handle ->
                handle.createUpdate("DELETE FROM chunk_claims WHERE world = :world AND chunk_x = :chunkX AND chunk_z = :chunkZ")
                    .bind("world", claim.world)
                    .bind("chunkX", claim.chunkX)
                    .bind("chunkZ", claim.chunkZ)
                    .execute()
            }
            claims.remove(claim)
        }
    }
}