/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.assy.demand.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository
import org.simplemes.mes.demand.domain.Order

/**
 * The repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface OrderBOMComponentRepository extends BaseRepository, CrudRepository<OrderBOMComponent, UUID> {

  Optional<OrderBOMComponent> findByUuid(UUID uuid)

  List<OrderBOMComponent> findAllByOrder(Order order)

  List<OrderBOMComponent> list()

}
