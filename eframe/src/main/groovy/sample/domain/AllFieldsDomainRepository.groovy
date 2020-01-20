/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import io.micronaut.data.annotation.Join
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample AllFieldsDomain repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface AllFieldsDomainRepository extends BaseRepository, CrudRepository<AllFieldsDomain, UUID> {

  Optional<AllFieldsDomain> findByName(String name)

  List<AllFieldsDomain> findAllByName(String name)

  Optional<AllFieldsDomain> findByUuid(UUID uuid)

  List<AllFieldsDomain> list()

  // Test that joins the order reference.  This only works for required references.
  @Join(value = "order", type = Join.Type.LEFT_FETCH)
  Optional<AllFieldsDomain> findByTitle(String title)

}
