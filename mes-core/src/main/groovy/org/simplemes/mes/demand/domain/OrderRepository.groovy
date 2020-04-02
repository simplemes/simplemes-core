/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.demand.domain

import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The Order repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface OrderRepository extends BaseRepository, CrudRepository<Order, UUID> {

  Optional<Order> findByOrder(String order)

  Optional<Order> findByUuid(UUID uuid)

  List<Order> findAllByDateCompletedLessThan(Date date, Pageable pageable)

  List<Order> list(Pageable pageable)

  List<Order> list()

  long count()

}
