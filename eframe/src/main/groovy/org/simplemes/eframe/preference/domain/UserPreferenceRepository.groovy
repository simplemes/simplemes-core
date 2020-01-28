/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.preference.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample UserPreference repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface UserPreferenceRepository extends BaseRepository, CrudRepository<UserPreference, UUID> {
  Optional<UserPreference> findByUuid(UUID uuid)

  Optional<UserPreference> findByUserNameAndPage(String userName, String page)

  List<UserPreference> list()
}
