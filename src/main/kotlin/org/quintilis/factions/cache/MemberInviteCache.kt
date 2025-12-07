package org.quintilis.factions.cache

import org.quintilis.factions.dao.MemberInviteDao
import org.quintilis.factions.managers.ConfigManager
import org.quintilis.factions.managers.RedisManager
import java.lang.Exception
import java.util.UUID

class MemberInviteCache(
    private val memberInviteDao: MemberInviteDao,
) {
    private val KEY_PREFIX = "factions:invites:player:"
    private val TTL_SECONDS = ConfigManager.getMaxInvitationTime() * 60L

    fun getClanNames(playerId: UUID): List<String>{
        val key = "$KEY_PREFIX$playerId"

        return try{
            RedisManager.run { jedis->
                if(jedis.exists(key)){
                    return@run jedis.smembers(key).toList()
                }

                val clanNames = memberInviteDao.findClanNamesForInvites(playerId)

                if(clanNames.isNotEmpty()){
                    jedis.sadd(key, *clanNames.toTypedArray())
                    jedis.expire(key, TTL_SECONDS)
                }
                clanNames
            }
        }catch (e: Exception){
            e.printStackTrace()
            memberInviteDao.findClanNamesForInvites(playerId)
        }
    }
}