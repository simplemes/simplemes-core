/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample FieldExtension repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface FieldExtensionRepository extends BaseRepository, CrudRepository<FieldExtension, UUID> {
  Optional<FieldExtension> findByUuid(UUID uuid)
  Optional<FieldExtension> findByFieldName(String fieldName)
  List<FieldExtension> findAllByDomainClassName(String domainClassName)
  List<FieldExtension> list()
}
