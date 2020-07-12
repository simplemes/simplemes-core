/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The FlexType repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface FlexTypeRepository extends BaseRepository, CrudRepository<FlexType, UUID> {
  Optional<FlexType> findByUuid(UUID uuid)
  Optional<FlexType> findByFlexType(String flexType)

  List<FlexType> list(Pageable pageable)
  List<FlexType> list()
}
