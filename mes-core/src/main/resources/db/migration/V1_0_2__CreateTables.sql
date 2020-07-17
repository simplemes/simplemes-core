/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

CREATE TABLE public.lsn_sequence (
    uuid             uuid PRIMARY KEY,
    sequence         character varying(30)  NOT NULL,
    title            character varying(80),
    format_string    character varying(200) NOT NULL,
    current_sequence bigint                 NOT NULL,
    default_sequence boolean                NOT NULL,
    fields           jsonb,
    date_created     timestamp with time zone,
    date_updated     timestamp with time zone,
    version          integer                NOT NULL,
    UNIQUE (sequence)
);

CREATE TABLE public.order_sequence (
    uuid             uuid PRIMARY KEY,
    sequence         character varying(30)  NOT NULL,
    title            character varying(80),
    format_string    character varying(200) NOT NULL,
    current_sequence bigint                 NOT NULL,
    default_sequence boolean                NOT NULL,
    fields           jsonb,
    date_created     timestamp with time zone,
    date_updated     timestamp with time zone,
    version          integer                NOT NULL,
    UNIQUE (sequence)
);



CREATE TABLE public.product (
    uuid                uuid PRIMARY KEY,
    product             character varying(128) NOT NULL,
    title               character varying(80),
    description         text,
    lsn_tracking_option character varying(16)  NOT NULL,
    lsn_sequence_id     uuid REFERENCES lsn_sequence,
    lot_size            numeric                NOT NULL,
    master_routing_id   uuid,
    fields              jsonb,
    date_created        timestamp with time zone,
    date_updated        timestamp with time zone,
    version             integer                NOT NULL,
    UNIQUE (product)
);

CREATE TABLE public.product_operation (
    uuid       uuid PRIMARY KEY,
    product_id uuid,
    sequence   integer               NOT NULL,
    title      character varying(80) NOT NULL,
    fields     jsonb
);

CREATE TABLE public.master_routing (
    uuid         uuid PRIMARY KEY,
    routing      character varying(128) NOT NULL,
    title        character varying(80),
    fields       jsonb,
    date_created timestamp with time zone,
    date_updated timestamp with time zone,
    version      integer                NOT NULL,
    UNIQUE (routing)
);

CREATE TABLE public.master_operation (
    uuid              uuid PRIMARY KEY,
    master_routing_id uuid,
    sequence          integer               NOT NULL,
    title             character varying(80) NOT NULL,
    fields            jsonb,
    UNIQUE (master_routing_id, sequence)
);


CREATE TABLE public.ordr (
    uuid                uuid PRIMARY KEY,
    ordr                varchar(30) NOT NULL,
    overall_status      varchar(8)  NOT NULL,
    qty_to_build        numeric     NOT NULL,
    qty_released        numeric     NOT NULL,
    product_id          uuid REFERENCES product,
    lsn_tracking_option varchar(16) NOT NULL,
    date_released       timestamp with time zone,
    date_completed      timestamp with time zone,
    qty_in_queue        numeric     NOT NULL,
    qty_in_work         numeric     NOT NULL,
    qty_done            numeric     NOT NULL,
    date_qty_queued     timestamp with time zone,
    date_qty_started    timestamp with time zone,
    date_first_queued   timestamp with time zone,
    date_first_started  timestamp with time zone,
    fields              jsonb,
    date_created        timestamp with time zone,
    date_updated        timestamp with time zone,
    version             integer     NOT NULL,
    UNIQUE (ordr)
);

CREATE TABLE public.order_oper_state (
    uuid               uuid PRIMARY KEY,
    sequence           integer NOT NULL,
    order_id           uuid,
    qty_in_queue       numeric NOT NULL,
    qty_in_work        numeric NOT NULL,
    qty_done           numeric NOT NULL,
    date_qty_queued    timestamp with time zone,
    date_qty_started   timestamp with time zone,
    date_first_queued  timestamp with time zone,
    date_first_started timestamp with time zone,
    fields             jsonb,
    date_created       timestamp with time zone,
    date_updated       timestamp with time zone,
    UNIQUE (order_id, sequence)
);

CREATE TABLE public.order_operation (
    uuid     uuid PRIMARY KEY,
    order_id uuid,
    sequence integer               NOT NULL,
    title    character varying(80) NOT NULL,
    fields   jsonb,
    UNIQUE (order_id, sequence)
);



CREATE TABLE public.lsn (
    uuid               uuid PRIMARY KEY,
    lsn                varchar(50) NOT NULL,
    order_id           uuid REFERENCES ordr,
    status             varchar(8)  NOT NULL,
    qty                numeric     NOT NULL,
    qty_in_queue       numeric     NOT NULL,
    qty_in_work        numeric     NOT NULL,
    qty_done           numeric     NOT NULL,
    date_qty_queued    timestamp with time zone,
    date_qty_started   timestamp with time zone,
    date_first_queued  timestamp with time zone,
    date_first_started timestamp with time zone,
    fields             jsonb,
    date_created       timestamp with time zone,
    date_updated       timestamp with time zone,
    version            integer     NOT NULL,
    UNIQUE (lsn, order_id)
);

CREATE TABLE public.lsn_oper_state (
    uuid               uuid PRIMARY KEY,
    sequence           integer NOT NULL,
    lsn_id             uuid,
    qty_in_queue       numeric NOT NULL,
    qty_in_work        numeric NOT NULL,
    qty_done           numeric NOT NULL,
    date_qty_queued    timestamp with time zone,
    date_qty_started   timestamp with time zone,
    date_first_queued  timestamp with time zone,
    date_first_started timestamp with time zone,
    fields             jsonb,
    date_created       timestamp with time zone,
    date_updated       timestamp with time zone,
    version            integer NOT NULL,
    UNIQUE (lsn_id, sequence)
);


CREATE TABLE public.action_log (
    uuid           uuid PRIMARY KEY,
    action         varchar(30) NOT NULL,
    date_time      timestamp with time zone,
    user_name      varchar(30) NOT NULL,
    order_id       uuid,
    lsn_id         uuid,
    qty            numeric,
    product_id     uuid,
    work_center_id uuid,
    fields         jsonb,
    date_created   timestamp with time zone
);
CREATE INDEX IF NOT EXISTS action_log_order
    ON public.action_log(order_id);
CREATE INDEX IF NOT EXISTS action_log_lsn
    ON public.action_log(lsn_id);

CREATE TABLE public.production_log (
    uuid               uuid PRIMARY KEY,
    action             character varying(30) NOT NULL,
    date_time          timestamp with time zone,
    start_date_time    timestamp with time zone,
    elapsed_time       bigint                NOT NULL,
    user_name          character varying(30) NOT NULL,
    ordr               character varying(30),
    lsn                character varying(50),
    product            character varying(128),
    master_routing     character varying(128),
    operation_sequence integer,
    work_center        character varying(30),
    qty                numeric               NOT NULL,
    qty_started        numeric               NOT NULL,
    qty_completed      numeric               NOT NULL,
    date_created       timestamp with time zone,
    fields             jsonb
);

CREATE INDEX IF NOT EXISTS production_log_order
    ON public.production_log(ordr);

CREATE TABLE public.work_center (
    uuid           uuid PRIMARY KEY,
    work_center    character varying(30) NOT NULL,
    title          character varying(80),
    overall_status character varying(8)  NOT NULL,
    fields         jsonb,
    date_created   timestamp with time zone,
    date_updated   timestamp with time zone,
    version        integer               NOT NULL,
    UNIQUE (work_center)
);
