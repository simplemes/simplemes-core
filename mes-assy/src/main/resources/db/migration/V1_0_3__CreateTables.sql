/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

CREATE TABLE public.order_assembled_component (
    uuid                 uuid PRIMARY KEY,
    order_id             uuid REFERENCES ordr  NOT NULL,
    lsn_id               uuid REFERENCES lsn,
    sequence             integer               NOT NULL,
    bom_sequence         integer               NOT NULL,
    location             character varying(30),
    component_id         uuid REFERENCES product,
    assembly_data_id     uuid REFERENCES flex_type,
    fields               jsonb,
    qty                  numeric,
    user_name            character varying(30) NOT NULL,
    work_center_id       uuid REFERENCES work_center,
    state                character varying(16) NOT NULL,
    removed_by_user_name character varying(30),
    removed_date         timestamp with time zone,
    date_created         timestamp with time zone,
    date_updated         timestamp with time zone,
    UNIQUE (order_id, sequence)
);

CREATE TABLE public.order_bom_component (
    uuid         uuid PRIMARY KEY,
    order_id     uuid REFERENCES ordr    NOT NULL,
    sequence     integer                 NOT NULL,
    component_id uuid REFERENCES product NOT NULL,
    qty          numeric                 NOT NULL,
    UNIQUE (order_id, sequence)
);

CREATE TABLE public.product_component (
    uuid         uuid PRIMARY KEY,
    product_id   uuid REFERENCES product,
    sequence     integer                 NOT NULL,
    component_id uuid REFERENCES product NOT NULL,
    qty          numeric                 NOT NULL,
    UNIQUE (product_id, sequence)
);
