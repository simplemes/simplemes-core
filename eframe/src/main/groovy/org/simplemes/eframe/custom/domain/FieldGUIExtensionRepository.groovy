/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The FieldGUIExtension repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface FieldGUIExtensionRepository extends BaseRepository, CrudRepository<FieldGUIExtension, UUID> {
  Optional<FieldGUIExtension> findByUuid(UUID uuid)

  Optional<FieldGUIExtension> findByDomainName(String domainName)
  List<FieldGUIExtension> list()
}
