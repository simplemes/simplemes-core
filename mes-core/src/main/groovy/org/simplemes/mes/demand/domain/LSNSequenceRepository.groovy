/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.demand.domain

import io.micronaut.data.annotation.Query
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The LSNSequence repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface LSNSequenceRepository extends BaseRepository, CrudRepository<LSNSequence, UUID> {

  Optional<LSNSequence> findBySequence(String sequence)

  Optional<LSNSequence> findByDefaultSequence(Boolean defaultSequence)

  @Query("select * from lsn_sequence o where o.uuid = :uuid for update ")
  Optional<LSNSequence> findByUuidWithLock(UUID uuid)

  Optional<LSNSequence> findByUuid(UUID uuid)

  List<LSNSequence> list(Pageable pageable)

  List<LSNSequence> list()

}
