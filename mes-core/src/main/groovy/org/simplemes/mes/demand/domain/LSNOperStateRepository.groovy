/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.demand.domain


import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample LSNOperState repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface LSNOperStateRepository extends BaseRepository, CrudRepository<LSNOperState, UUID> {

  Optional<LSNOperState> findByUuid(UUID uuid)

  List<LSNOperState> findAllByLsn(LSN lsn)

  List<LSNOperState> list()

}
