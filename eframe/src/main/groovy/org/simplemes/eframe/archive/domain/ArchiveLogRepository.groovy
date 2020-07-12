/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.archive.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The ArchiveLog repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ArchiveLogRepository extends BaseRepository, CrudRepository<ArchiveLog, UUID> {
  Optional<ArchiveLog> findByUuid(UUID uuid)

  List<ArchiveLog> list()
}
