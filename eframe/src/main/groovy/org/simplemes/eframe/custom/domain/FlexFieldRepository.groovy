/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The FlexField repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface FlexFieldRepository extends BaseRepository, CrudRepository<FlexField, UUID> {
  Optional<FlexField> findByUuid(UUID uuid)

  List<FlexField> findAllByFlexType(FlexType flexType)
  List<FlexField> list()
}
