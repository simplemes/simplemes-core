/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The FieldExtension repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface FieldExtensionRepository extends BaseRepository, CrudRepository<FieldExtension, UUID> {
  Optional<FieldExtension> findByUuid(UUID uuid)

  Optional<FieldExtension> findByFieldName(String fieldName)
  List<FieldExtension> findAllByDomainClassName(String domainClassName)
  List<FieldExtension> list()
}
