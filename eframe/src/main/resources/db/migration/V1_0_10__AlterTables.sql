/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */


ALTER TABLE public.flex_field
    add column history_tracking varchar(30);
ALTER TABLE public.flex_field
    add column required boolean default false;
ALTER TABLE public.field_extension
    add column required boolean default false;
ALTER TABLE public.field_extension
    add column history_tracking varchar(30);

