/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample order repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface OrderLineRepository extends BaseRepository, CrudRepository<OrderLine, UUID> {

  List<OrderLine> findAllByOrder(Order order)

  Optional<OrderLine> findByUuid(UUID uuid)

  List<OrderLine> list()

}
