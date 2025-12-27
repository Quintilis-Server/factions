package org.quintilis.factions.entities.log

/**
 * Tipos de ação que podem ser registradas no log.
 */
enum class ActionLogType(val description: String) {
    // Clan actions
    CLAN_CREATE("Clan created"),
    CLAN_DELETE("Clan deleted"),
    CLAN_LEADER_TRANSFER("Clan leader transferred"),
    
    // Member actions
    MEMBER_INVITE("Member invited"),
    MEMBER_JOIN("Member joined"),
    MEMBER_LEAVE("Member left"),
    MEMBER_KICK("Member kicked"),
    
    // Ally actions
    ALLY_INVITE_SEND("Ally invite sent"),
    ALLY_INVITE_ACCEPT("Ally invite accepted"),
    ALLY_INVITE_REJECT("Ally invite rejected"),
    ALLY_REMOVE("Ally removed"),
    
    // Territory actions
    CHUNK_CLAIM("Chunk claimed"),
    CHUNK_UNCLAIM("Chunk unclaimed"),
    
    // Market actions
    MARKET_LISTING_CREATE("Market listing created"),
    MARKET_LISTING_SOLD("Market listing sold"),
    MARKET_LISTING_CANCEL("Market listing cancelled"),
    
    // Player actions
    PLAYER_FIRST_JOIN("Player first join"),
    PLAYER_POINTS_TRANSFER("Points transferred"),
    
    // Admin actions
    ADMIN_POINTS_GIVE("Admin gave points"),
    ADMIN_POINTS_TAKE("Admin took points")
}
