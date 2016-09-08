drop database if exists jsk;
drop user if exists jsk;
create user jsk with login superuser inherit unencrypted password 'jsk';
create database jsk owner jsk;
alter database jsk set search_path to "$user", public;

\c jsk;

set schema 'public';

-- Boiler Plate definitions
create table "user" (
  id serial primary key
, first_name varchar(50) not null
, last_name varchar(50) not null
, password_digest char(100) null -- nullable ie if you use social login
, created_at timestamp not null default now()
, created_by integer not null
, updated_at timestamp not null default now()
, updated_by integer not null
);

insert into "user"(first_name, last_name, created_by, updated_by)
  values ('System', 'User', 1, 1);

create table channel_type (
    id serial primary key
  , name varchar(20) not null
  , created_at timestamp not null default now()
  , created_by integer not null references "user"(id)
);

create unique index unq_channel_type_name on channel_type(name);

insert into channel_type (name, created_by)
  values ('mobile',1), ('email',1), ('google',1);

create table channel (
    id serial primary key
  , user_id integer not null references "user"(id)
  , channel_type_id integer not null references channel_type(id)
  , identifier varchar(50) not null -- email address, google id, phone #, etc
  , token varchar(50) null -- token used for verification
  , token_expiration timestamp null
  , verified_at timestamp null
  , created_at timestamp not null default now()
  , created_by integer not null references "user"(id)
  , updated_at timestamp not null default now()
  , updated_by integer not null references "user"(id)
);

create unique index unq_channel_channel_type_identifier on channel(channel_type_id,identifier);
create index idx_channel_user_id on channel(user_id);

create table address (
   id serial primary key
 , street_1    varchar(50)
 , street_2    varchar(20)
 , city        varchar(50)    not null
 , state       varchar(50)    not null
 , postal_code varchar(9)
 , lat         decimal(9,6)
 , lng         decimal(9,6)
 , created_at  timestamp      not null default now()
 , created_by  integer        not null references "user"(id)
 , updated_at  timestamp      not null default now()
 , updated_by  integer        not null references "user"(id)
);

--
-- Auth
--
create table "permission" (
id serial primary key
, name varchar(50) not null
, description varchar(100) not null
, created_at timestamp not null default now()
, created_by integer not null references "user"(id)
, updated_at timestamp not null default now()
, updated_by integer not null references "user"(id)
);

create unique index unq_permission_name on "permission"(name);

create table "role" (
    id serial primary key
  , name varchar(50) not null
  , description varchar(100) not null
  , created_at timestamp not null default now()
  , created_by integer not null references "user"(id)
  , updated_at timestamp not null default now()
  , updated_by integer not null references "user"(id)
);

create unique index unq_role_name on "role"(name);

create table role_permission (
    id serial primary key
  , role_id integer not null references role(id)
  , permission_id integer not null references permission(id)
  , created_at timestamp not null default now()
  , created_by integer not null references "user"(id)
);

create unique index unq_role_permission on role_permission(role_id, permission_id);

create table user_role (
    id serial primary key
  , user_id integer not null references "user"(id)
  , role_id integer not null references role(id)
  , created_at timestamp not null default now()
  , created_by integer not null references "user"(id)
);

create unique index unq_user_role on user_role(user_id, role_id);


-- JSK Definitions
create table agent(
   id serial primary key
 , name varchar(50) not null
 , created_at  timestamp     not null default now()
 , created_by  integer       not null references "user"(id)
 , updated_at  timestamp     not null default now()
 , updated_by  integer       not null references "user"(id)
);

create table node_type(
   id   serial primary key
 , name varchar(20) not null
 , description varchar(50) not null
 , created_at timestamp not null default now()
 , created_by  integer       not null references "user"(id)
);

create unique index unq_node_type on node_type(name);
insert into node_type (name,description,created_by) values('job', 'Job',1), ('workflow','Workflow',1);

create table node(
   id           serial        primary key
 , node_type_id integer       not null references node_type(id)
 , name         varchar(100)  not null
 , description  varchar(255)
 , is_enabled   boolean       not null default true
 , tags         jsonb         not null default '[]'::jsonb
 , created_at   timestamp     not null default now()
 , created_by   integer       not null references "user"(id)
 , updated_at   timestamp     not null default now()
 , updated_by   integer       not null references "user"(id)
);

create table variable(
  id           serial        primary key
, node_id      integer       not null references node(id)
, name         varchar(100)  not null
, value        varchar(2000) not null
, is_env       boolean       not null default false
, created_at   timestamp     not null default now()
, created_by   integer       not null references "user"(id)
, updated_at   timestamp     not null default now()
, updated_by   integer       not null references "user"(id)
);

create unique index unq_variable on variable(node_id, name);


create table behavior(
   id          serial        primary key
 , name        varchar(50)   not null
 , description varchar(255)  not null
 , created_at  timestamp     not null default now()
 , created_by  integer       not null references "user"(id)
);

create unique index unq_behavior on behavior(name);
insert into behavior(name,description,created_by) values ('standard', 'Standard',1), ('loop','Loop',1), ('cond', 'Conditional', 1);

create table job (
   id             serial       primary key
 , node_id        integer not  null references node(id)
 , agent_id       integer      null references agent(id)
 , type           varchar(50)  not null -- shell, jdbc, ftp, http etc
 , props          jsonb   not  null default '{}'::jsonb
 , behavior_id    integer not  null references behavior(id)
 , context_key    varchar(100) null -- for loop, context_key is required and is the key for the value, avail in downstream jobs
 , max_retries    integer not  null default 1 -- nbr of retries before job is considered failed
 , max_concurrent integer not  null default 1 -- how many to run at once if
 , timeout        integer not  null default 0 -- timeout milliseconds, fails job, 0 means no timeout
);

create table workflow(
    id      serial primary key
  , node_id integer not null references node(id)
);

insert into node(node_type_id,name,description,is_enabled,created_by,updated_by)
  select
         nt.id
       , 'SYSTEM-WF'
       , 'Workflow implicitly used by jobs when not in an explicit workflow.'
       , true
       , 1
       , 1
    from node_type nt
   where nt.name = 'workflow';

insert into workflow(node_id)
  select n.id
    from node n
    join node_type nt
      on n.node_type_id = nt.id
   where nt.name = 'SYSTEM-WF';


-- NB.  can't have unique constraints on this
-- a job being used simultaneously, used again, etc.
create table dependency(
   id                       serial    primary key
 , workflow_id              integer   not null references workflow(id)
 , node_id                  integer   not null references node(id)
 , next_node_id             integer   references node(id)
 , is_next_node_on_success  boolean   not null default true
 , created_at               timestamp not null default now()
 , created_by               integer   not null references "user"(id)
 , updated_at               timestamp not null default now()
 , updated_by               integer   not null references "user"(id)
);

-- Alerts
create table alert(
   id               serial        primary key
 , name             varchar(50)   not null
 , description      varchar(255)
 , is_for_failure   boolean       not null default true
 , created_at       timestamp     not null default now()
 , created_by       integer       not null references "user"(id)
 , updated_at       timestamp     not null default now()
 , updated_by       integer       not null references "user"(id)
);

create unique index unq_alert on alert(name);

create table alert_channel(
   id          serial        primary key
 , alert_id    integer       not null references alert(id)
 , channel_id  integer       not null references channel(id)
 , created_at  timestamp     not null default now()
 , created_by  integer       not null references "user"(id)
 , updated_at  timestamp     not null default now()
 , updated_by  integer       not null references "user"(id)
);

create unique index unq_alert_channel on alert_channel(alert_id, channel_id);

create table node_alert(
   id          serial        primary key
 , node_id     integer       not null references node(id)
 , alert_id    integer       not null references alert(id)
 , created_at  timestamp     not null default now()
 , created_by  integer       not null references "user"(id)
 , updated_at  timestamp     not null default now()
 , updated_by  integer       not null references "user"(id)
);

create unique index unq_node_alert on node_alert(node_id, alert_id);

create table directory (
   id         serial       primary key
 , name       varchar(100) not null
 , parent_id  integer     references directory(id) -- root dir won't have parent
 , created_at timestamp   not null default now()
 , created_by integer     not null references "user"(id)
 , updated_at timestamp   not null default now()
 , updated_by integer     not null references "user"(id)
);

create unique index unq_directory on directory(name, parent_id);

insert into directory(name, created_by, updated_by) values ('/', 1, 1);

create table directory_node(
   id            serial    primary key
 , directory_id  integer   not null references directory(id)
 , node_id       integer   not null references node(id)
 , created_at    timestamp not null default now()
 , created_by    integer   not null references "user"(id)
 , updated_at    timestamp not null default now()
 , updated_by    integer   not null references "user"(id)
);

create unique index unq_directory_node on directory_node(node_id);

-- ideally there'd be a unique constraint on cron_expr but
-- maybe too restrictive
create table schedule(
   id          serial        primary key
 , name        varchar(100)  not null
 , description varchar(255)
 , cron_expr   varchar(100)  not null
 , created_at  timestamp     not null default now()
 , created_by  integer       not null references "user"(id)
 , updated_at  timestamp     not null default now()
 , updated_by  integer       not null references "user"(id)
);

create unique index unq_schedule_name on schedule(name);

create table node_schedule(
   id          serial        primary key
 , node_id     integer       not null references node(id)
 , schedule_id integer       not null references schedule(id)
 , created_at  timestamp     not null default now()
 , created_by  integer       not null references "user"(id)
 , updated_at  timestamp     not null default now()
 , updated_by  integer       not null references "user"(id)
);

create table status(
   id          serial      primary key
 , name        varchar(20) not null
 , description varchar(50) not null
);

create unique index unq_status on status(name);

insert into status (id, name, description)
       values (1, 'unexecuted', 'Unexecuted')
            , (2, 'started', 'Started')
            , (3, 'finished-success', 'Finished successfully')
            , (4, 'finished-errored', 'Finished with errors')
            , (5, 'aborted', 'Aborted')
            , (6, 'unknown', 'Unknown')
            , (7, 'pending', 'Pending')
            , (8, 'forced-success', 'Forced Success')
            , (9, 'paused', 'Paused')
            , (10, 'timed-out', 'Timed Out');

-- Execution
create table ex(
  id           serial    primary key
, status_id    integer   not null references status(id)
, started_at   timestamp
, finished_at  timestamp
, created_at   timestamp not null default now()
, updated_at   timestamp not null default now()
);

create table ex_node(
  id               serial        primary key
, ex_id            integer       not null references ex(id)
, original_node_id integer       not null -- no fk, in case original node_id is deleted, for reporting
, node_type_id     integer       not null references node_type(id)
, name             varchar(100)  not null
, description      varchar(255)
, is_enabled       boolean       not null
, tags             jsonb         not null
, variables        jsonb         not null
, created_at       timestamp     not null
);

create table ex_variable(
  id           serial        primary key
, ex_node_id   integer       not null references ex_node(id)
, name         varchar(100)  not null
, value        varchar(2000) not null
, is_env       boolean       not null
, created_at   timestamp     not null default now()
);

create unique index unq_ex_variable on ex_variable(ex_node_id, name);

-- snapshot of actual job for historical purposes
create table ex_job(
   id             serial         primary key
 , ex_node_id     integer        not null references ex_node(id)
 , agent_name     varchar(50)
 , type           varchar(50)    not null
 , props          jsonb          not null
 , behavior_id    integer        not null references behavior(id)
 , context_key    varchar(100)   null
 , max_retries    integer        not null
 , max_concurrent integer        not null
 , timeout        integer        not null
 , created_at     timestamp      not null default now()
);

create table ex_workflow(
   id          serial    primary key
 , ex_node_id  integer   not null references ex_node(id)
 , created_at  timestamp not null default now()
);

create table ex_instance(
   id               serial      primary key
 , ex_workflow_id   integer     not null references ex_workflow(id)
 , ex_node_id       integer     not null references ex_node(id)
 , group_identifier varchar(25) not null -- set each time a loop node is hit for nodes in the loop, uuid for eg
 , variables        jsonb       not null -- merged map of resolved variables from node, parent wfs, loops, etc dir heirarchy in order of preference
 , context          jsonb       not null -- a json map
 , created_at       timestamp   not null default now()
);

create table ex_instance_status(
  id                  serial    primary key
, ex_instance_id      integer   not null references ex_instance(id)
, status_id           integer   not null references status(id)
, created_at          timestamp not null default now()
);

create table ex_dependency(
  id                          serial     primary key
, ex_workflow_id              integer    not null references ex_workflow(id)
, ex_instance_id              integer    not null references ex_instance(id)
, ex_next_instance_id         integer    references ex_instance(id)
, is_next_instance_on_success boolean    not null
, created_at                  timestamp  not null default now()
);
