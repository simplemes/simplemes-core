/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.archive.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample ArchiveLog repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface ArchiveLogRepository extends BaseRepository, CrudRepository<ArchiveLog, UUID> {
  Optional<ArchiveLog> findByUuid(UUID uuid)

  List<ArchiveLog> list()
}
