package org.quintilis.factions.handlers

import org.bukkit.Bukkit
import org.quintilis.factions.entities.clan.ClanEntity
import org.quintilis.factions.services.FactionsServices.chunkCache
import org.quintilis.factions.services.FactionsServices.coreCache

fun deleteCores(clan: ClanEntity){
    val cores =coreCache.findByClanId(clan.id!!)
    for(core in cores){
        chunkCache.findByClanId(clan.id).forEach { chunk -> chunkCache.unclaimChunk(chunk.id!!) }


    }
}