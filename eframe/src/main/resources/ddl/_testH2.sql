/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

/*SQL executed on test startup.  Not used in production. */

ALTER TABLE flex_type ADD CONSTRAINT ft_key1 UNIQUE (flex_type);
ALTER TABLE field_extension ADD CONSTRAINT fe_key1 UNIQUE (domain_class_name,field_name);