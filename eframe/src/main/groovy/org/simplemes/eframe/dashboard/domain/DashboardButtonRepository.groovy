/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample DashboardButton repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface DashboardButtonRepository extends BaseRepository, CrudRepository<DashboardButton, UUID> {
  Optional<DashboardButton> findByUuid(UUID uuid)

  List<DashboardButton> findAllByDashboardConfig(DashboardConfig dashboardConfig)

  List<DashboardButton> list()
}
