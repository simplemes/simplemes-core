/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.product.domain

import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample MasterRouting repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface MasterRoutingRepository extends BaseRepository, CrudRepository<MasterRouting, UUID> {

  Optional<MasterRouting> findByRouting(String routing)

  Optional<MasterRouting> findByUuid(UUID uuid)

  List<MasterRouting> list(Pageable pageable)

  List<MasterRouting> list()

}
