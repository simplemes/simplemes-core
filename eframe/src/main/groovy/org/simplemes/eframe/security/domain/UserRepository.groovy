package org.simplemes.eframe.security.domain

import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The sample order repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface UserRepository extends BaseRepository, CrudRepository<User, UUID> {

  @Join(value = "userRoles", type = Join.Type.LEFT_FETCH)
  Optional<User> findByUserName(String userName)

  Optional<User> findById(UUID uuid)

  Optional<User> findByUuid(UUID uuid)

  List<User> list()

  @Join(value = "userRoles", type = Join.Type.LEFT_FETCH)
  User get(UUID uuid)


}
