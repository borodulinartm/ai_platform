create table admin_category
(
    id           serial
        primary key,
    name         varchar(191) not null
        unique,
    kind         smallint default 0,
    "lastUpdate" bigint   default 0,
    error        smallint default 0,
    attributes   text
);