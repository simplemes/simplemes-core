/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample SampleChild repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface SampleChildRepository extends BaseRepository, CrudRepository<SampleChild, UUID> {
  Optional<SampleChild> findByUuid(UUID uuid)
  List<SampleChild> list()
  List<SampleChild> findAllBySampleParent(SampleParent sampleParent)
}
