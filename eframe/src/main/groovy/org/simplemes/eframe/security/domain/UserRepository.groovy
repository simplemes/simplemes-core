/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain

import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The User repository base interface.  Provides the methods for the repo.
 */
@SuppressWarnings("unused")
@JdbcRepository(dialect = Dialect.POSTGRES)
interface UserRepository extends BaseRepository, CrudRepository<User, UUID> {

  @Join(value = "userRoles", type = Join.Type.LEFT_FETCH)
  Optional<User> findByUserName(String userName)

  Optional<User> findById(UUID uuid)

  Optional<User> findByUuid(UUID uuid)

  Optional<User> findByUserNameAndEnabled(String userName, Boolean enabled)

  List<User> list(Pageable pageable)
  List<User> list()

  @Join(value = "userRoles", type = Join.Type.LEFT_FETCH)
  User get(UUID uuid)


}
