/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample CustomOrderComponent repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface CustomOrderComponentRepository extends BaseRepository, CrudRepository<CustomOrderComponent, UUID> {
  List<CustomOrderComponent> findAllByOrder(Order order)

  Optional<CustomOrderComponent> findById(UUID uuid)

  List<CustomOrderComponent> list()

}
