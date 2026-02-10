package org.quintilis.factions.services

import org.bukkit.Chunk
import org.bukkit.entity.Player
import org.quintilis.factions.entities.chunk.ChunkEntity
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.entities.log.ActionLogEntity
import org.quintilis.factions.entities.log.ActionLogType
import org.quintilis.factions.results.Result
import org.quintilis.factions.entities.clan.ClanCoreEntity
import java.util.UUID

/**
 * Serviço para gerenciamento de chunks de clãs.
 * Centraliza lógica de claim/unclaim de territórios.
 */
class ChunkService {
    
    private val chunkDao get() = Services.chunkDao
    private val chunkCache get() = Services.chunkCache
    private val clanCache get() = Services.clanCache
    
    /**
     * Reivindica um chunk para um clã.
     * 
     * @param player Jogador que está reivindicando
     * @param clan Clã que vai possuir o chunk
     * @param chunk Chunk do Bukkit a ser reivindicado
     * @param transactionId ID da transação de pagamento (se houver)
     * @return ClanResult indicando sucesso ou erro
     */
    fun claimChunk(
        player: Player,
        clan: ClanEntity,
        chunk: Chunk,
        core: ClanCoreEntity,
        transactionId: Int? = null
    ): Result {
        val worldUuid = chunk.world.uid
        val centerX = chunk.x
        val centerZ = chunk.z

        var isConnected = chunkCache.getChunkCount(clan.id!!) == 0
        //tenta achar o id do chunk

        val corners = listOf(
            Pair(centerX - 1, centerZ - 1),
            Pair(centerX + 1, centerZ - 1),
            Pair(centerX - 1, centerZ + 1),
            Pair(centerX + 1, centerZ + 1)
        )

        val checkOffsets = listOf(
            Pair(0, 1),  // Checa pra Cima (Z+)
            Pair(0, -1), // Checa pra Baixo (Z-)
            Pair(1, 0),  // Checa pra Direita (X+)
            Pair(-1, 0)  // Checa pra Esquerda (X-)
        )

        for(checkX in (centerX-1)..(centerX+1)){
            for(checkZ in (centerZ-1)..(centerZ+1)){

                //Checagem de ocupação
                val ownerId = chunkCache.getChunkOwner(worldUuid, checkX, checkZ)

                if(ownerId != null){
                    if(ownerId != clan.id){
                        return Result.Error("chunk.error.too_close_to_enemy")
                    }else{
                        isConnected = true
                    }
                }
            }
            if(!isConnected) {
                for((cornerX, cornerZ) in corners){
                    for((offX, offZ) in checkOffsets){
                        val neighborX = cornerX + offX
                        val neighborZ = cornerZ + offZ

                        if(neighborX in (centerX-1)..(centerX+1) && neighborZ in (centerZ-1)..(centerZ+1)){
                            continue
                        }

                        if(chunkCache.getChunkOwner(worldUuid, neighborX, neighborZ) == clan.id){
                            isConnected = true
                            break
                        }
                    }
                    if(isConnected) break
                }
            }
            if(!isConnected){
                return Result.Error("chunk.error.not_connected")
            }
        }


        return Result.Success("chunk.success")
    }


    private fun saveChunk(chunk: Chunk): ChunkEntity {
        return ChunkEntity(chunk).save()
    }
    
    /**
     * Libera um chunk de um clã.
     * 
     * @param player Jogador que está liberando
     * @param clan Clã que possui o chunk
     * @param chunk Chunk do Bukkit a ser liberado
     * @return ClanResult indicando sucesso ou erro
     */
    fun unclaimChunk(
        player: Player,
        clan: ClanEntity,
        chunk: Chunk
    ): Result {
        val worldUuid = chunk.world.uid
        val x = chunk.x
        val z = chunk.z
        
        // Verifica se o chunk pertence ao clã
        val currentOwner = chunkCache.getChunkOwner(worldUuid, x, z)
        if (currentOwner == null) {
            return Result.Error("chunk.error.not_claimed")
        }
        if (currentOwner != clan.id) {
            return Result.Error("chunk.error.not_owned")
        }
        
        // Busca o chunk
        val chunkEntity = chunkCache.getChunk(worldUuid, x, z)
            ?: return Result.Error("error.generic")
        
        // Libera o chunk
        try {
            chunkDao.unclaimChunk(chunkEntity.id!!)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.Error("error.generic")
        }
        
        // Invalida caches
        chunkCache.invalidateChunkAndClan(worldUuid, x, z, clan.id)
        
        // Log da ação
        ActionLogEntity.log(
            actionType = ActionLogType.CHUNK_UNCLAIM,
            actorId = player.uniqueId,
            clanId = clan.id!!,
            details = "Unclaimed chunk at $x, $z in ${chunk.world.name}"
        )
        
        return Result.Success(
            "chunk.unclaim.response",
            mapOf("x" to x, "z" to z)
        )
    }
    
    /**
     * Libera todos os chunks de um clã.
     * Usado quando o clã é deletado.
     * 
     * @param clanId ID do clã
     * @return Número de chunks liberados
     */
    fun unclaimAllChunks(clanId: Int): Int {
        val count = chunkDao.unclaimAllChunksByClan(clanId)
        chunkCache.invalidateClanChunks(clanId)
        return count
    }
    
    /**
     * Verifica se um chunk está reivindicado.
     */
    fun isClaimed(worldUuid: UUID, x: Int, z: Int): Boolean {
        return chunkCache.isClaimed(worldUuid, x, z)
    }
    
    /**
     * Retorna o ID do clã dono de um chunk.
     */
    fun getChunkOwner(worldUuid: UUID, x: Int, z: Int): Int? {
        return chunkCache.getChunkOwner(worldUuid, x, z)
    }
    
    /**
     * Retorna o clã dono de um chunk.
     */
    fun getChunkOwnerClan(worldUuid: UUID, x: Int, z: Int): ClanEntity? {
        val ownerId = chunkCache.getChunkOwner(worldUuid, x, z) ?: return null
        return clanCache.getClan(ownerId)
    }
    
    /**
     * Retorna todos os chunks de um clã.
     */
    fun getChunksByClan(clanId: Int): List<ChunkEntity> {
        return chunkCache.getChunksByClan(clanId)
    }
    
    /**
     * Retorna a quantidade de chunks de um clã.
     */
    fun getChunkCount(clanId: Int): Int {
        return chunkCache.getChunkCount(clanId)
    }
}
