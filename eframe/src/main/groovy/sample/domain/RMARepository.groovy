/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The RMA repository base interface.  Provides the methods for the repo.
 */
@SuppressWarnings('unused')
@JdbcRepository(dialect = Dialect.POSTGRES)
interface RMARepository extends BaseRepository, CrudRepository<RMA, UUID> {
  Optional<RMA> findByRma(String rma)

  Optional<RMA> findByUuid(UUID uuid)

  List<RMA> list()
}
