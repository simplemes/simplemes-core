/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample DashboardConfig repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface DashboardConfigRepository extends BaseRepository, CrudRepository<DashboardConfig, UUID> {
  Optional<DashboardConfig> findByUuid(UUID uuid)

  Optional<DashboardConfig> findByDashboard(String dashboard)

  Optional<DashboardConfig> findByCategoryAndDefaultConfig(String category, Boolean defaultConfig)

  List<DashboardConfig> findAllByCategoryAndDefaultConfig(String category, Boolean defaultConfig)

  List<DashboardConfig> findAllByDefaultConfig(Boolean defaultConfig)

  List<DashboardConfig> list()
}
