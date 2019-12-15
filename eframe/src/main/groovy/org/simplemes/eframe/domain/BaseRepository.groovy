package org.simplemes.eframe.domain

import io.micronaut.data.annotation.RepositoryConfiguration
import io.micronaut.data.annotation.TypeRole
import io.micronaut.data.jdbc.mapper.SqlResultConsumer
import io.micronaut.data.model.query.builder.sql.SqlQueryBuilder

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines common repository features.  Mainly used to define the configuration that supports
 * added features.
 */

@RepositoryConfiguration(queryBuilder = SqlQueryBuilder.class,
  operations = EFrameJdbcRepositoryOperations.class,
  implicitQueries = false,
  namedParameters = false,
  typeRoles = @TypeRole(
    role = SqlResultConsumer.ROLE,
    type = SqlResultConsumer.class
  ))
interface BaseRepository {
}