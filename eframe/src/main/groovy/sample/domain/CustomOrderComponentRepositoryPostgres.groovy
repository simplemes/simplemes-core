/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

/**
 * The sample CustomOrderComponent repository base interface.  Provides the methods for the repo,
 * for production or dev (POSTGRES)
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface CustomOrderComponentRepositoryPostgres extends CustomOrderComponentRepository {
}
