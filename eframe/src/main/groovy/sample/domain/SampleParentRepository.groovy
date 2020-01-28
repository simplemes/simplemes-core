/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain


import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample SampleParent repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface SampleParentRepository extends BaseRepository, CrudRepository<SampleParent, UUID> {
  Optional<SampleParent> findByUuid(UUID uuid)
  Optional<SampleParent> findByName(String name)
  List<SampleParent> list()
}
