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
 * but a sub-class must be defined as a dialect-specific repo.
 */
interface Order2Repository extends BaseRepository, CrudRepository<Order2, UUID> {

  Optional<Order2> findByOrder(String order)

  Optional<Order2> findById(UUID uuid)

/*
  @Join(value = "product", type = Join.Type.LEFT_FETCH)
  Optional<Order> findById(UUID uuid)

  @Join(value = "product", type = Join.Type.LEFT_FETCH)
  Optional<Order> find(String order)

  @Join(value = "orderLines", type = Join.Type.LEFT_FETCH, alias = "ol_")
  Order findByOrder(String order)

  @Join(value = "orderLines", type = Join.Type.LEFT_FETCH, alias = "ol_")
  List<Order> list()
*/

  List<Order2> list()

  //@Join(value = "order", type = Join.Type.LEFT_FETCH, alias = "ol_")
  //List<Order> list()
}
