/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */


CREATE TABLE public.all_fields_domain (
    uuid                 uuid PRIMARY KEY,
    name                 varchar(255) NOT NULL,
    title                varchar(255),
    qty                  numeric,
    count                integer,
    int_primitive        integer,
    enabled              boolean,
    date_time            timestamp with time zone,
    due_date             date,
    display_only_text    varchar(255),
    notes                varchar(255),
    another_field        varchar(255),
    report_time_interval varchar(255),
    order_id             uuid,
    status               varchar(255),
    other_custom_fields  jsonb,
    date_created         timestamp with time zone,
    date_updated         timestamp with time zone,
    version              integer      NOT NULL
);

CREATE TABLE public.ordr (
    uuid          uuid PRIMARY KEY,
    ordr          varchar(255) NOT NULL,
    qty_to_build  numeric      NOT NULL,
    product       varchar(255),
    status        varchar(255),
    due_date      date,
    date_created  timestamp with time zone,
    date_updated  timestamp with time zone,
    notes         varchar(255),
    custom_fields jsonb,
    version       integer      NOT NULL
);

CREATE TABLE public.custom_order_component (
    uuid                 uuid PRIMARY KEY,
    order_id             uuid,
    sequence             integer NOT NULL,
    qty                  numeric NOT NULL,
    product              varchar(255),
    notes                varchar(255),
    foreign_reference_id uuid,
    assy_data_type_id    uuid,
    custom_fields jsonb
);

CREATE TABLE public.order_line (
    uuid     uuid PRIMARY KEY,
    order_id uuid,
    sequence integer NOT NULL,
    qty      numeric NOT NULL,
    product  varchar(255),
    notes    varchar(255)
);

CREATE TABLE public.rma (
    uuid          uuid PRIMARY KEY,
    rma           varchar(255) NOT NULL,
    status        varchar(255) NOT NULL,
    product       varchar(255),
    qty           numeric      NOT NULL,
    return_date   date,
    rma_type_id   uuid,
    date_created  timestamp with time zone,
    date_updated  timestamp with time zone,
    version       integer      NOT NULL,
    custom_fields jsonb
);

CREATE TABLE public.sample_child (
    uuid                 uuid PRIMARY KEY,
    sample_parent_id     uuid,
    key_value            varchar(255) NOT NULL,
    sequence             integer,
    title                varchar(255),
    format               varchar(255),
    qty                  numeric,
    enabled              boolean,
    date_time            timestamp with time zone,
    due_date             date,
    report_time_interval varchar(255),
    order_id             uuid
);

CREATE TABLE public.sample_grand_child (
    uuid            uuid PRIMARY KEY,
    sample_child_id uuid,
    grand_key       varchar(255) NOT NULL,
    title           varchar(255)
);

CREATE TABLE public.sample_parent (
    uuid                 uuid PRIMARY KEY,
    name                 varchar(40) NOT NULL,
    title                varchar(20),
    notes                varchar(255),
    not_displayed        varchar(255),
    more_notes           varchar(255),
    date_created         timestamp with time zone,
    date_updated         timestamp with time zone,
    all_fields_domain_id uuid,
    custom_fields        jsonb,
    version              integer     NOT NULL
);

CREATE TABLE public.sample_parent_all_fields_domain (
    sample_parent_id     uuid NOT NULL,
    all_fields_domain_id uuid NOT NULL
);
