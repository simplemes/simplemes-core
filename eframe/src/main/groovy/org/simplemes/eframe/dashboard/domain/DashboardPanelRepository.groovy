/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The DashboardPanel repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface DashboardPanelRepository extends BaseRepository, CrudRepository<DashboardPanel, UUID> {
  Optional<DashboardPanel> findByUuid(UUID uuid)

  List<DashboardPanel> findAllByDashboardConfig(DashboardConfig dashboardConfig)

  List<DashboardPanel> list()
}
