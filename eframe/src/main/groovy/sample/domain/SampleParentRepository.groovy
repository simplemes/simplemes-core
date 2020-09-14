/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The SampleParent repository base interface.  Provides the methods for the repo.
 */
@SuppressWarnings('unused')
@JdbcRepository(dialect = Dialect.POSTGRES)
interface SampleParentRepository extends BaseRepository, CrudRepository<SampleParent, UUID> {
  Optional<SampleParent> findByUuid(UUID uuid)

  Optional<SampleParent> findByName(String name)

  List<SampleParent> findAllByNameLike(String name, Pageable pageable)

  List<SampleParent> list(Pageable pageable)

  List<SampleParent> list()

}
