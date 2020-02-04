/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample DashboardPanel repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface DashboardPanelRepository extends BaseRepository, CrudRepository<DashboardPanel, UUID> {
  Optional<DashboardPanel> findByUuid(UUID uuid)

  List<DashboardPanel> findAllByDashboardConfig(DashboardConfig dashboardConfig)

  List<DashboardPanel> list()
}
