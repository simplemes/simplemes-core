/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The Role repository base interface.  Provides the methods for the repo.
 */
@SuppressWarnings("unused")
@JdbcRepository(dialect = Dialect.POSTGRES)
interface RoleRepository extends BaseRepository, CrudRepository<Role, UUID> {

  Optional<Role> findByAuthority(String authority)

  Optional<Role> findById(UUID uuid)

  Optional<Role> findByUuid(UUID uuid)

  List<Role> list()

}
