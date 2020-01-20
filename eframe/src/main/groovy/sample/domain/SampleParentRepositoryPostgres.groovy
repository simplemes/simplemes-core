/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.simplemes.eframe.misc.EFEnvironments

/**
 * The sample repository base interface.  Provides the methods for the repo,
 * for production or dev (POSTGRES)
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
@Requires(env = [EFEnvironments.PRODUCTION, Environment.DEVELOPMENT])
interface SampleParentRepositoryPostgres extends SampleParentRepository {
}
