create table anticore
(
    id                  serial
        constraint anticore_pk
            primary key,
    clan_id             integer                                            not null
        constraint anticore_clans_id_fk
            references clans
            on update cascade on delete cascade,
        x               integer,
    y                   integer,
    z                   integer,
    placed              boolean                 default false             not null,
    placed_at           timestamp with time zone,
    created_at          timestamp with time zone default CURRENT_TIMESTAMP not null,
    target_core_id      integer
        constraint anticore_clan_cores_id_fk
            references clan_cores,
    active              BOOL                    DEFAULT TRUE                NOT NULL,
    shots               INTEGER                 DEFAULT 10                  NOT NULL,
    shots_left          INTEGER                 DEFAULT 10                  NOT NULL,
    world_uuid          uuid,
    glowstone_charges   integer                 DEFAULT 0                   NOT NULL
);

-- CREATE OR REPLACE FUNCTION sync_anticore_active()
--     RETURNS TRIGGER AS $$
-- BEGIN
--     IF NEW.shots_left <= 0 THEN
--         NEW.active := false;
--     ELSE
--         NEW.active := true;
--     END IF;
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- CREATE TRIGGER trg_sync_active
--     BEFORE UPDATE ON anticore
--     FOR EACH ROW EXECUTE FUNCTION sync_anticore_active();
