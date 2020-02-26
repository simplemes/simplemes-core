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

  // Note: Since LSN is not unique by itself, the only finder by LSN may return multiples.
  List<LSN> findAllByLsn(String lsn)

  List<LSN> findAllByOrder(Order order)

  List<LSN> list()

}
