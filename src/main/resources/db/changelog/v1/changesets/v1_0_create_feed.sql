create table admin_feed
(
    id                serial
        primary key,
    url               varchar(32768)           not null,
    kind              smallint      default 0,
    category          integer       default 0
                                               references admin_category
                                                   on update cascade on delete set null,
    name              varchar(191)             not null,
    website           varchar(32768),
    description       text,
    "lastUpdate"      bigint        default 0,
    priority          smallint      default 10 not null,
    "pathEntries"     varchar(4096) default NULL::character varying,
    "httpAuth"        varchar(1024) default NULL::character varying,
    error             smallint      default 0,
    ttl               integer       default 0  not null,
    attributes        text,
    "cache_nbEntries" integer       default 0,
    "cache_nbUnreads" integer       default 0
);

create index admin_name_index
    on admin_feed (name);

create index admin_priority_index
    on admin_feed (priority);