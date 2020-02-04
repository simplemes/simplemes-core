/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample DashboardPanelSplitter repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface DashboardPanelSplitterRepository extends BaseRepository, CrudRepository<DashboardPanelSplitter, UUID> {
  Optional<DashboardPanelSplitter> findByUuid(UUID uuid)

  List<DashboardPanelSplitter> findAllByDashboardConfig(DashboardConfig dashboardConfig)

  List<DashboardPanelSplitter> list()
}
