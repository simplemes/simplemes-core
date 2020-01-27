/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

/**
 * The sample repository base interface.  Provides the methods for the repo,
 * for production or dev (POSTGRES)
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface SampleParentRepositoryPostgres extends SampleParentRepository {
}
