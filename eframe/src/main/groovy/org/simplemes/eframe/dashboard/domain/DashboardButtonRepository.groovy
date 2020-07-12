/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The DashboardButton repository base interface.  Provides the methods for the repo.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface DashboardButtonRepository extends BaseRepository, CrudRepository<DashboardButton, UUID> {
  Optional<DashboardButton> findByUuid(UUID uuid)

  List<DashboardButton> findAllByDashboardConfig(DashboardConfig dashboardConfig)

  List<DashboardButton> list()
}
