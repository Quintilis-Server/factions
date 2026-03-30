-- Players
CREATE TABLE players(
    id          UUID PRIMARY KEY,
    name        varchar(255)        NOT NULL,
    points      INTEGER DEFAULT 0   NOT NULL
);

-- Chunk
CREATE TABLE chunk(
    id          SERIAL      PRIMARY KEY,
    world_uuid  UUID        NOT NULL,
    chunk_x     INTEGER     NOT NULL,
    chunk_z     INTEGER     NOT NULL
);

-- Clans
CREATE TABLE clans(
    id              SERIAL PRIMARY KEY                                  NOT NULL,
    name            VARCHAR(30)                                         NOT NULL,
    tag             VARCHAR(5),
    leader_uuid     UUID                                                NOT NULL
        REFERENCES players,
    active          BOOLEAN DEFAULT TRUE                                NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    points          INTEGER DEFAULT 0                                   NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE clan_relation(
    id              SERIAL PRIMARY KEY,
    clan1_id        INTEGER                 NOT NULL
        REFERENCES clans,
    clan2_id        INTEGER                 NOT NULL
        REFERENCES clans,
    relation        VARCHAR(32)             NOT NULL,
    active          BOOLEAN DEFAULT TRUE    NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uidx_clan1_id_clan2_id
    ON clan_relation(clan1_id, clan2_id) WHERE active = TRUE;

CREATE TABLE clan_member(
    id              SERIAL PRIMARY KEY,
    clan_id         INTEGER                                             NOT NULL
        REFERENCES clans,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    active          BOOLEAN DEFAULT TRUE                                NOT NULL,
    player_id       uuid                                                NOT NULL
        REFERENCES players,
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uidx_active_member
    ON clan_member(player_id) WHERE active = TRUE;

CREATE TABLE clan_cores(
    id              SERIAL PRIMARY KEY,
    x               INTEGER,
    y               INTEGER,
    z               INTEGER,
    clan_id         INTEGER
        REFERENCES clans,
    health          INTEGER                     DEFAULT 1000                            NOT NULL,
    active          BOOLEAN                     DEFAULT TRUE                            NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE    DEFAULT CURRENT_TIMESTAMP               NOT NULL,
    type            VARCHAR(16)                 DEFAULT 'SUB_CORE'                      NOT NULL,
    parent_core     INTEGER
        REFERENCES clan_cores,
    placed          BOOLEAN                     DEFAULT FALSE                           NOT NULL,
    placed_at       TIMESTAMP WITH TIME ZONE,

    CHECK ( (placed = FALSE) OR ( (x IS NOT NULL) AND (y IS NOT NULL) AND (z IS NOT NULL) ) )
);

-- Garantir que só exista UM "NEXUS" ativo por clã. (Permite vários SUB_CORES)
CREATE UNIQUE INDEX
    ON clan_cores(clan_id)
    WHERE ( (active = true) AND (type = 'NEXUS') );

-- ==============================================================
-- MOVIDO PARA CIMA: transactions (clan_chunk depende dela)
-- ==============================================================
CREATE TABLE transactions(
     id                                          SERIAL PRIMARY KEY      NOT NULL,
     player_id                                   UUID                    NOT NULL,
     transaction_type                            VARCHAR(255)            NOT NULL
         CHECK (((transaction_type)::text <> 'TRANSFER_RECEIVE'::text) OR ((parent_id IS NOT NULL) AND (change >= 0)))
         CHECK ( ((transaction_type)::text <> 'TRANSFER_TAKE') OR (change < 0) ),
     change                                      INTEGER                 NOT NULL,
     time                                        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
     parent_id                                   INTEGER
         REFERENCES transactions,
     market_transaction_detail_transaction_id    INTEGER UNIQUE
    -- TODO: A tabela 'market_transaction_details' não existe neste script.
    -- Comentei a FK para não quebrar a execução. Se a tabela existir, descomente abaixo:
    -- REFERENCES market_transaction_details
);

-- Agora podemos criar clan_chunk com segurança
CREATE TABLE clan_chunk(
    id                  SERIAL      PRIMARY KEY     NOT NULL,
    chunk_id            INTEGER                     NOT NULL
       REFERENCES chunk,
    clan_id             INTEGER                     NOT NULL
       REFERENCES clans,
    active              BOOLEAN     DEFAULT TRUE    NOT NULL,
    transaction_id      INTEGER                     NOT NULL
       REFERENCES transactions,
    owner_core          INTEGER                     NOT NULL
       REFERENCES clan_cores
);

CREATE UNIQUE INDEX uidx_chunk_id
    ON clan_chunk(chunk_id)
    WHERE active = TRUE;

-- Invite
CREATE TABLE ally_invite(
    id              SERIAL      PRIMARY KEY,
    sender_clan_id  INTEGER                         NOT NULL
        REFERENCES clans,
    target_clan_id  INTEGER                         NOT NULL
        REFERENCES clans,
    created_at      TIMESTAMP WITH TIME ZONE
                                DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    active          BOOLEAN     DEFAULT TRUE        NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING'   NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE        NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE member_invite(
    id              SERIAL      PRIMARY KEY                                     NOT NULL,
    clan_id         INTEGER                                                     NOT NULL
        REFERENCES clans,
    player_id       UUID                                                        NOT NULL
        REFERENCES players,
    created_at      TIMESTAMP WITH TIME ZONE
                              DEFAULT CURRENT_TIMESTAMP                             NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE
                              DEFAULT (CURRENT_TIMESTAMP + '5 MINUTES':: INTERVAL)  NOT NULL,
    active          BOOLEAN     DEFAULT TRUE                                    NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING'                               NOT NULL
);

CREATE UNIQUE INDEX uidx_player_id
    ON member_invite(clan_id, player_id)
    WHERE active = TRUE;

-- LOGS
CREATE TABLE chat_logs(
    id              BIGSERIAL       PRIMARY KEY             NOT NULL,
    player_id       UUID                                    NOT NULL
        REFERENCES players,
    content         TEXT                                    NOT NULL,
    channel         VARCHAR(32)                             NOT NULL,
    recipient_id    uuid                                    NOT NULL
        REFERENCES players,
    clan_id         INTEGER                                 NOT NULL
        REFERENCES clans,
    location_x      INTEGER,
    location_z      INTEGER,
    world_uuid      UUID,
    created_at      TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP           NOT NULL
);

CREATE INDEX idx_clan_id
    ON chat_logs(clan_id);

CREATE INDEX idx_create_at
    ON chat_logs(created_at);

CREATE INDEX idx_player_id
    ON chat_logs(player_id);

CREATE TABLE deaths(
    id          SERIAL      PRIMARY KEY         NOT NULL,
    player_id   UUID                            NOT NULL
        REFERENCES players,
    killer_id   uuid                            NOT NULL
        REFERENCES players,
    created_at  TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP    NOT NULL
);

CREATE TABLE action_log
(
    id          SERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    actor_id    UUID NOT NULL,
    target_id   UUID,
    clan_id     INTEGER
        REFERENCES clans,
    details     TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_action_log_action_type
    ON action_log(action_type);

CREATE INDEX idx_action_log_actor
    ON action_log(actor_id);

CREATE INDEX idx_action_log_created
    ON action_log(created_at);

CREATE INDEX idx_action_log_clan
    ON action_log(clan_id);

-- War logs
CREATE TABLE war_logs(
     id                  BIGSERIAL PRIMARY KEY,
     attacker_id         UUID                            NOT NULL,
     attacker_name       VARCHAR                         NOT NULL,
     attacker_clan_id    INTEGER                         NOT NULL
         REFERENCES clans,
     defender_clan_id    INTEGER                         NOT NULL
         REFERENCES clans,
     target_core         INTEGER                         NOT NULL
         REFERENCES clan_cores,
     damage              INTEGER                         NOT NULL,
     remaining_health    INTEGER                         NOT NULL,
     x                   INTEGER                         NOT NULL,
     y                   INTEGER                         NOT NULL,
     z                   INTEGER                         NOT NULL,
     created_at          TIMESTAMP WITH TIME ZONE
         DEFAULT CURRENT_TIMESTAMP   NOT NULL
);

-- TRIGGER
CREATE FUNCTION check_clan_relation_overlap() RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF EXISTS (
        SELECT 1 FROM clan_relation
        WHERE active = TRUE
            AND (
                (clan1_id = NEW.clan2_id AND clan2_id = NEW.clan1_id)
                OR
                (clan1_id = NEW.clan1_id AND clan2_id = NEW.clan2_id)
            )
          AND id IS DISTINCT FROM NEW.id
    ) THEN
        RAISE EXCEPTION 'Já existe uma relação ativa entre os clas % e % em qualquer direção', NEW.clan1_id, NEW.clan2_id;
    end if;

    RETURN NEW;
end;
$$;

create trigger trg_validate_clan_relation
    before insert or update
    on clan_relation
    for each row
    when (new.active = true)
execute procedure check_clan_relation_overlap();