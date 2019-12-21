package sample.domain


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
interface OrderRepository extends BaseRepository, CrudRepository<Order, UUID> {

  Optional<Order> findByOrder(String order)

  Optional<Order> findById(UUID uuid)

  List<Order> list()

}
