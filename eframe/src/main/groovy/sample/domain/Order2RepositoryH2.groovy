package sample.domain

import io.micronaut.context.annotation.Requires
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The H2-Specific version of the repo.
 */
@JdbcRepository(dialect = Dialect.H2)
@Requires(env = ["test"])
interface Order2RepositoryH2 extends Order2Repository, CrudRepository<Order, UUID> {
}
