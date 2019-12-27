package org.simplemes.eframe.security.domain


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
@SuppressWarnings("unused")
interface RoleRepository extends BaseRepository, CrudRepository<Role, UUID> {

  Optional<Role> findByAuthority(String authority)

  Optional<Role> findById(UUID uuid)

  Optional<Role> findByUuid(UUID uuid)

  List<Role> list()

}
