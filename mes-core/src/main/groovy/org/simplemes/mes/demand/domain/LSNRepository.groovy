/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.demand.domain


import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The LSN repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface LSNRepository extends BaseRepository, CrudRepository<LSN, UUID> {

  Optional<LSN> findByUuid(UUID uuid)

  List<LSN> list(Pageable pageable)

  /**
   * Finds by LSN.
   * <b>Note: </b>Since LSN is not unique by itself, this finder may return the wrong LSN.  You need to verify it is correct.
   * Use the {@link org.simplemes.mes.demand.service.ResolveService#fixLSN(java.lang.Object)} resolve the cases when
   * the LSN might not be unique.
   * @param lsn The LSN to look for.
   * @return The LSN.
   */
  Optional<LSN> findByLsn(String lsn)

  List<LSN> findAllByLsn(String lsn)

  List<LSN> findAllByOrder(Order order)

  List<LSN> list()

}
