package sample.domain

import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The H2-Specific version of the repo.
 */
@JdbcRepository(dialect = Dialect.H2)
@Requires(env = [Environment.TEST])
interface OrderLineRepositoryH2 extends OrderLineRepository {
}
