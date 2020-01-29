/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample RMA repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface RMARepository extends BaseRepository, CrudRepository<RMA, UUID> {
  Optional<RMA> findByRma(String rma)

  Optional<RMA> findByUuid(UUID uuid)

  List<RMA> list()
}
