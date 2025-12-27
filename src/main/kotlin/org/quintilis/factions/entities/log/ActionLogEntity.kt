package org.quintilis.factions.entities.log

import org.quintilis.factions.entities.BaseEntity
import org.quintilis.factions.entities.annotations.Column
import org.quintilis.factions.entities.annotations.PrimaryKey
import org.quintilis.factions.entities.annotations.TableName
import java.time.Instant
import java.util.UUID

/**
 * Entidade para registro de ações do servidor.
 * Usada para logs e integração com n8n.
 */
@TableName("action_log")
data class ActionLogEntity(
    @PrimaryKey
    @Column("id")
    val id: Int?,
    
    @Column("action_type")
    val actionType: String,
    
    @Column("actor_id")
    val actorId: UUID,
    
    @Column("target_id")
    val targetId: UUID? = null,
    
    @Column("clan_id")
    val clanId: Int? = null,
    
    @Column("details")
    val details: String? = null,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
) : BaseEntity() {
    
    companion object {
        /**
         * Cria e salva um log de ação.
         */
        fun log(
            actionType: ActionLogType,
            actorId: UUID,
            targetId: UUID? = null,
            clanId: Int? = null,
            details: String? = null
        ): ActionLogEntity {
            return ActionLogEntity(
                id = null,
                actionType = actionType.name,
                actorId = actorId,
                targetId = targetId,
                clanId = clanId,
                details = details
            ).save()
        }
    }
}
