/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.preference.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The UserPreference repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface UserPreferenceRepository extends BaseRepository, CrudRepository<UserPreference, UUID> {
  Optional<UserPreference> findByUuid(UUID uuid)

  Optional<UserPreference> findByUserNameAndPage(String userName, String page)

  List<UserPreference> list()
}
