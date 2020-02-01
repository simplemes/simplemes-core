/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample FlexField repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface FlexFieldRepository extends BaseRepository, CrudRepository<FlexField, UUID> {
  Optional<FlexField> findByUuid(UUID uuid)
  List<FlexField> findAllByFlexType(FlexType flexType)
  List<FlexField> list()
}
