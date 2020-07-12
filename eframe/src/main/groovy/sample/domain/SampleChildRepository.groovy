/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The SampleChild repository base interface.  Provides the methods for the repo.
 */
@SuppressWarnings('unused')
@JdbcRepository(dialect = Dialect.POSTGRES)
interface SampleChildRepository extends BaseRepository, CrudRepository<SampleChild, UUID> {
  Optional<SampleChild> findByUuid(UUID uuid)
  List<SampleChild> list()
  List<SampleChild> findAllBySampleParent(SampleParent sampleParent)
}
