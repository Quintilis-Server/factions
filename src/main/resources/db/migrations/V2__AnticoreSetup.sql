create table anticore
(
    id             serial
        constraint anticore_pk
            primary key,
    clan_id        integer                                            not null
        constraint anticore_clans_id_fk
            references clans
            on update cascade on delete cascade,
    x              integer,
    y              integer,
    z              integer,
    placed         boolean                  default false             not null,
    placed_at      timestamp with time zone,
    created_at     timestamp with time zone default CURRENT_TIMESTAMP not null,
    target_core_id integer
        constraint anticore_clan_cores_id_fk
            references clan_cores
);
