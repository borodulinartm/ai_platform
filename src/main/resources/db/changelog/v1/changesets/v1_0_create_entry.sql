create table admin_entry
(
    id                 bigint             not null
        primary key,
    guid               varchar(767)       not null,
    title              varchar(8192)      not null,
    author             varchar(1024),
    content            text,
    link               varchar(16383)     not null,
    date               bigint,
    "lastSeen"         bigint   default 0,
    "lastUserModified" bigint   default 0,
    hash               bytea,
    is_read            smallint default 0 not null,
    is_favorite        smallint default 0 not null,
    id_feed            integer
        references admin_feed
            on update cascade on delete cascade,
    tags               varchar(2048),
    attributes         text,
    unique (id_feed, guid)
);

create index admin_is_favorite_index
    on admin_entry (is_favorite);

create index admin_is_read_index
    on admin_entry (is_read);

create index "admin_entry_lastSeen_index"
    on admin_entry ("lastSeen");

create index admin_entry_feed_read_index
    on admin_entry (id_feed, is_read);

create index admin_entry_last_user_modified_index
    on admin_entry ("lastUserModified");