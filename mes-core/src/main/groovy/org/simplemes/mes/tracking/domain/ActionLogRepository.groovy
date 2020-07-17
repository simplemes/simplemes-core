/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.tracking.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order

/**
 * The ActionLog repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ActionLogRepository extends BaseRepository, CrudRepository<ActionLog, UUID> {

  Optional<ActionLog> findByUuid(UUID uuid)

  List<ActionLog> list(Pageable pageable)

  List<ActionLog> list()

  List<ActionLog> findAllByOrder(Order order)

  List<ActionLog> findAllByLsn(LSN lsn)

}
