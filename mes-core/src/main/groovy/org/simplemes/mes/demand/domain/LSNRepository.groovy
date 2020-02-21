/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.demand.domain


import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample LSN repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface LSNRepository extends BaseRepository, CrudRepository<LSN, UUID> {

  Optional<LSN> findByUuid(UUID uuid)

  List<LSN> list(Pageable pageable)

  List<LSN> list()

}
