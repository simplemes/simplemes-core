/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample FlexType repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface FlexTypeRepository extends BaseRepository, CrudRepository<FlexType, UUID> {
  Optional<FlexType> findByUuid(UUID uuid)
  Optional<FlexType> findByFlexType(String flexType)

  List<FlexType> list(Pageable pageable)
  List<FlexType> list()
}
