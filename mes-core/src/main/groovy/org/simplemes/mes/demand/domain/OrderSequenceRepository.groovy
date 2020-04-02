/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.demand.domain

import io.micronaut.data.annotation.Query
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The OrderSequence repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface OrderSequenceRepository extends BaseRepository, CrudRepository<OrderSequence, UUID> {

  Optional<OrderSequence> findBySequence(String sequence)

  Optional<OrderSequence> findByDefaultSequence(Boolean defaultSequence)

  @Query("select * from order_sequence o where o.uuid = :uuid for update ")
  Optional<OrderSequence> findByUuidWithLock(UUID uuid)

  Optional<OrderSequence> findByUuid(UUID uuid)

  List<OrderSequence> list(Pageable pageable)

  List<OrderSequence> list()

}
